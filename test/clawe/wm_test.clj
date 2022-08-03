(ns clawe.wm-test
  (:require
   [clojure.test :refer [deftest is testing] :as t]
   test-util ;; requiring to support 'valid as assert-expr
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.workspace :as workspace]
   [ralphie.zsh :as zsh]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest current-workspaces-schema-test
  (testing "conforms to malli schemas"
    (is (valid [:sequential workspace/schema] (wm/current-workspaces)))))

(deftest current-workspace-schema-test
  (testing "conforms to malli schemas"
    (is (valid workspace/schema (wm/current-workspace)))))

(deftest active-workspaces-schema-test
  (testing "conforms to malli schemas"
    (is (valid [:sequential workspace/schema] (wm/active-workspaces)))))

(deftest fetch-workspace-schema-test
  (testing "conforms to malli schemas"
    (is (valid workspace/schema
               (-> (wm/current-workspace) :workspace/title wm/fetch-workspace)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; behavior, integration tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE these could be impacted by background wm processes, may need delays/sleeps

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
        (is (= test-wsp-title
               (-> (workspace/all-active) first :workspace/title)))
        (is (= expected-dir
               (-> (workspace/all-active) first :workspace/directory)))))))
