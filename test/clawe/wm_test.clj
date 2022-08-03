(ns clawe.wm-test
  (:require
   [clojure.test :refer [deftest is testing] :as t]
   test-util ;; requiring to support 'valid as assert-expr
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.workspace :as workspace]
   [ralphie.zsh :as zsh]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]
   [clawe.client :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest current-workspaces-schema-test
  (is (valid [:sequential workspace/schema] (wm/current-workspaces))))

(deftest current-workspace-schema-test
  (is (valid workspace/schema (wm/current-workspace))))

(deftest active-workspaces-schema-test
  (is (valid [:sequential workspace/schema] (wm/active-workspaces))))

(deftest fetch-workspace-schema-test
  (is (valid workspace/schema
             (-> (wm/current-workspace) :workspace/title wm/fetch-workspace))))

(deftest active-clients-schema-test
  (is (valid [:sequential client/schema] (wm/active-clients))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; behavior, integration tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NOTE these may be impacted by background wm processes, may need delays/sleeps
;; for now most things are synchronous and fast

;; workspace crud

(deftest create-and-delete-workspace-test
  (let [initial-wsp-count   (count (wm/active-workspaces))
        new-workspace-title (str "test-wsp-" (random-uuid))]
    (is (nil? (wm/fetch-workspace new-workspace-title))
        "workspace does not exist before creating")
    (wm/ensure-workspace new-workspace-title)
    (is (valid workspace/schema (wm/fetch-workspace new-workspace-title)))
    (is (#{new-workspace-title} (-> (wm/fetch-workspace new-workspace-title) :workspace/title))
        "new wsp has matching title")
    (is (= (inc initial-wsp-count) (count (wm/active-workspaces)))
        "wsp count has incremented")
    (wm/delete-workspace (wm/fetch-workspace new-workspace-title))
    (is (nil? (wm/fetch-workspace new-workspace-title))
        "workspace does not exist after deleting")
    (is (= initial-wsp-count (count (wm/active-workspaces)))
        "wsp count is back")))

;; rearranging

(deftest swap-workspaces-by-index-test
  (testing "swap two workspaces, assert on the new indexes"
    (let [wsps (->> (wm/active-workspaces) (take 2))]
      (is (> (count wsps) 1) "Not enough workspaces to run test.")
      ;; consider shuffling before running, or running a few different times
      (let [[wsp1 wsp2 & _rest] wsps
            wsp1-index          (:workspace/index wsp1)
            wsp2-index          (:workspace/index wsp2)]
        (wm/swap-workspaces-by-index wsp1-index wsp2-index)

        (let [updated-wsp1 (wm/fetch-workspace (:workspace/title wsp1))
              updated-wsp2 (wm/fetch-workspace (:workspace/title wsp2))]
          (is (= wsp1-index (:workspace/index updated-wsp2)))
          (is (= wsp2-index (:workspace/index updated-wsp1))))

        ;; swap them back, order of indexes shouldn't matter
        (wm/swap-workspaces-by-index wsp2-index wsp1-index)
        (let [reupdated-wsp1 (wm/fetch-workspace (:workspace/title wsp1))
              reupdated-wsp2 (wm/fetch-workspace (:workspace/title wsp2))]
          (is (= wsp1-index (:workspace/index reupdated-wsp1)))
          (is (= wsp2-index (:workspace/index reupdated-wsp2))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current-workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord NoWorkspacesWM []
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] []))

(def test-wsp-title "my-wsp")
(defrecord OneWorkspaceWM []
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] [{:workspace/title test-wsp-title
                                       :workspace/index 1}])
  (-active-workspaces [_this _opts] [{:workspace/title test-wsp-title
                                      :workspace/index 1}]))

(deftest current-workspace-test
  (testing "has a title"
    (is (not (nil? (-> (wm/current-workspace) :workspace/title)))))
  (testing "has a directory"
    (is (not (nil? (-> (wm/current-workspace) :workspace/directory)))))

  (testing "validates against `workspace/schema`"
    (is (valid workspace/schema (wm/current-workspace))))

  (testing "returns nil" ;; formerly, sets a fallback title and directory
    (sys/with-system [wm/*wm* (NoWorkspacesWM.)]
      (is (nil? (wm/current-workspace))))
    #_(let [fallback {:workspace/title     "home"
                      :workspace/directory (zsh/expand "~")}]
        (sys/with-system [wm/*wm* (NoWorkspacesWM.)]
          (is (valid workspace/schema (wm/current-workspace)))
          (is (= fallback (wm/current-workspace))))))

  (testing "mixes and expands data from config/workspace-def"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system [wm/*wm* (OneWorkspaceWM.)]
        (reset! clawe.config/*config*
                {:workspace/defs
                 {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})
        (let [curr (wm/current-workspace)]
          (is (valid workspace/schema curr))
          (is (= test-wsp-title (curr :workspace/title)))
          (is (= expected-dir (curr :workspace/directory))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; active-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest all-active-test
  (testing "merges config def data"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [wm/*wm* (OneWorkspaceWM.)]
        (reset!
          clawe.config/*config*
          {:workspace/defs
           {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})
        (let [wsps (wm/active-workspaces)]
          (is (valid [:sequential workspace/schema] wsps))
          (is (= test-wsp-title
                 (-> wsps first :workspace/title)))
          (is (= expected-dir
                 (-> wsps first :workspace/directory))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace-defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest workspace-defs-test
  (testing "re-uses the map key as the title."
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (reset! clawe.config/*config*
              {:workspace/defs
               {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})
      (is (= test-wsp-title
             (-> (wm/workspace-defs) first :workspace/title)))
      (is (= expected-dir
             (-> (wm/workspace-defs) first :workspace/directory))))))
