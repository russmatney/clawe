(ns ^:integration clawe.wm-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures] :as t]
   [test-util :refer [wait-until]]
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.workspace :as workspace]
   [ralphie.zsh :as zsh]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]
   [clawe.client :as client]))

(use-fixtures :each
  (fn [run-test]
    (let [og-wsp (wm/current-workspace)]

      ;; TODO disable clawe.doctor before running

      (run-test)

      ;; reload the config in-case any tests messed with it
      (clawe.config/reload-config)

      (wm/focus-workspace og-wsp)
      (wait-until
        "Confirming we reverted to the og workspace" 1000
        (workspace/match? (wm/current-workspace) og-wsp)))))

(defn clawe-conf [defs]
  (atom {:client/defs defs}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest current-workspaces-schema-test
  (is (valid [:sequential workspace/schema] (wm/current-workspaces))))

(deftest current-workspaces-with-clients-schema-test
  (is (valid [:sequential workspace/schema]
             (wm/current-workspaces {:include-clients true}))))

(deftest current-workspace-schema-test
  (is (valid workspace/schema (wm/current-workspace))))

(deftest active-workspaces-schema-test
  (is (valid [:sequential workspace/schema] (wm/active-workspaces))))

(deftest active-workspaces-with-clients-schema-test
  (is (valid [:sequential workspace/schema]
             (wm/active-workspaces {:include-clients true}))))

(deftest fetch-workspace-schema-test
  (is (valid workspace/schema
             (-> (wm/current-workspace) :workspace/title wm/fetch-workspace))))

(deftest fetch-workspace-with-clients-schema-test
  (is (valid workspace/schema
             (->> (wm/current-workspace) :workspace/title
                  (wm/fetch-workspace {:include-clients true})))))

(deftest active-clients-schema-test
  (is (valid [:sequential client/schema] (wm/active-clients))))

(deftest fetch-client-schema-test
  (is (valid client/schema (-> (wm/active-clients) first wm/fetch-client))))

(deftest focused-client-schema-test
  (is (valid client/schema (wm/focused-client))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; behavior, integration tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NOTE these may be impacted by background wm processes, may need delays/sleeps
;; see `test-util/wait-until`
;; NOTE these tests should restore the original state when run
;; (i.e. don't focus some random wsp and leave us there)
;; see `use-fixtures`

;; workspace crud

(deftest create-and-delete-workspace-test
  (let [initial-wsp-count   (count (wm/active-workspaces))
        new-workspace-title (str "test-wsp-" (random-uuid))]
    (is (nil? (wm/fetch-workspace new-workspace-title))
        "workspace does not exist before creating")

    (wm/create-workspace new-workspace-title)
    (wait-until
      "New workspace being created" 1000
      (#{new-workspace-title} (-> (wm/fetch-workspace new-workspace-title) :workspace/title)))

    (is (valid workspace/schema (wm/fetch-workspace new-workspace-title)))
    (is (#{new-workspace-title} (-> (wm/fetch-workspace new-workspace-title) :workspace/title))
        "new wsp has matching title")
    (is (= (inc initial-wsp-count) (count (wm/active-workspaces)))
        "wsp count has incremented")

    (wm/delete-workspace (wm/fetch-workspace new-workspace-title))
    (wait-until
      "New workspace being deleted" 1000
      (nil? (wm/fetch-workspace new-workspace-title)))

    (is (nil? (wm/fetch-workspace new-workspace-title))
        "workspace does not exist after deleting")
    (is (= initial-wsp-count (count (wm/active-workspaces)))
        "wsp count is back")))

;; focus

(deftest focus-workspace-test
  (let [wsps     (wm/active-workspaces)
        to-focus (rand-nth wsps)
        og-focus (wm/current-workspace)]
    ;; focus new wsp
    (wm/focus-workspace to-focus)

    ;; TODO write a better wrapper for these dupe-y wait/assertions
    (wait-until
      "Confirming workspace switch finished" 1000
      (workspace/match? (wm/current-workspace) to-focus))
    (is (workspace/match? (wm/current-workspace) to-focus)
        "Focused workspace should match new-current workspace")

    ;; revert to original
    (wm/focus-workspace og-focus)
    (wait-until
      "Confirming workspace switch finished" 1000
      (workspace/match? (wm/current-workspace) og-focus))
    (is (workspace/match? (wm/current-workspace) og-focus)
        "Focused workspace should match original workspace")))

(deftest focus-client-test
  (let [clients  (wm/active-clients)
        og-focus (wm/focused-client)
        to-focus (rand-nth clients)]
    (is (valid client/schema og-focus))
    (is (:client/focused og-focus))
    (wm/focus-client to-focus)
    (let [focused-client (wm/focused-client)]
      (is (valid client/schema focused-client))
      (is (:client/focused focused-client))
      (is (client/match? focused-client to-focus)))

    (wm/focus-client og-focus)
    (let [focused-client (wm/focused-client)]
      (is (valid client/schema focused-client))
      (is (:client/focused focused-client))
      (is (client/match? focused-client og-focus)))
    ;; NOTE we're relying on the test fixture to restore the og workspace
    ))

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

(deftest drag-workspace-test
  (let [wsps (->> (wm/active-workspaces) (sort-by :workspace/index) (into []))]
    (is (> (count wsps) 2) "Not enough workspaces to run test.")
    (let [sec-wsp (second wsps)]
      (wm/focus-workspace sec-wsp)
      (wait-until
        "Waiting for selected workspace to be focused" 1000
        (workspace/match? (wm/current-workspace) sec-wsp))
      (let [curr-wsp (wm/current-workspace)]
        (is (workspace/match? curr-wsp sec-wsp))

        (wm/drag-workspace :dir/up)
        (wait-until
          "Waiting for drag to finish" 1000
          (= (inc (:workspace/index curr-wsp))
             (:workspace/index (wm/current-workspace))))

        (is (= (inc (:workspace/index curr-wsp))
               (:workspace/index (wm/current-workspace)))
            "Dragging up should increase the index")
        ;; could assert on the swapped-with wsp here

        (wm/drag-workspace :dir/down)
        (wait-until
          "Waiting for drag to finish" 1000
          (workspace/match? curr-wsp (wm/current-workspace)))
        (is (= (:workspace/index curr-wsp)
               (:workspace/index (wm/current-workspace)))
            "Dragging up then down should return to the same index")

        (wm/drag-workspace :dir/down)
        (wait-until
          "Waiting for drag to finish" 1000
          (= (dec (:workspace/index curr-wsp))
             (:workspace/index (wm/current-workspace))))
        (is (= (dec (:workspace/index curr-wsp))
               (:workspace/index (wm/current-workspace)))
            "Dragging down should decrease the index")

        (wm/drag-workspace :dir/up)
        (wait-until
          "Waiting for drag to finish" 1000
          (workspace/match? curr-wsp (wm/current-workspace)))
        (is (= (:workspace/index curr-wsp)
               (:workspace/index (wm/current-workspace)))
            "Dragging down then up should return to the same index")))))


(deftest move-client-to-workspace-test
  (let [[og-wsp target-wsp] (->> (wm/active-workspaces
                                   {:include-clients true})
                                 (filter (comp seq :workspace/clients))
                                 (take 2))
        client              (-> og-wsp :workspace/clients first)]
    (is target-wsp)
    (is client)

    (wm/move-client-to-workspace client target-wsp)
    (wait-until
      "Client has been moved to target workspace" 1000
      (workspace/find-matching-client
        (wm/fetch-workspace {:include-clients true} target-wsp)
        (wm/fetch-client client)))
    (is (workspace/find-matching-client
          (wm/fetch-workspace {:include-clients true} target-wsp)
          (wm/fetch-client client))
        "target workspace should now contain the client")

    ;; move it back
    (wm/move-client-to-workspace client og-wsp)
    (wait-until
      "Client has been returned" 1000
      (workspace/find-matching-client
        (wm/fetch-workspace {:include-clients true} og-wsp)
        (wm/fetch-client client)))
    (is (workspace/find-matching-client
          (wm/fetch-workspace {:include-clients true} og-wsp)
          (wm/fetch-client client))
        "Client was returned to the current workspace")))

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

  (testing "returns nil"
    (sys/with-system [wm/*wm* (NoWorkspacesWM.)]
      (is (nil? (wm/current-workspace)))))

  (testing "mixes and expands data from config/workspace-def"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [wm/*wm* (OneWorkspaceWM.)
         clawe.config/*config*
         (atom
           {:workspace/defs
            {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})]
        (let [curr (wm/current-workspace)]
          (is (valid workspace/schema curr))
          (is (= test-wsp-title (curr :workspace/title)))
          (is (= expected-dir (curr :workspace/directory))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; active-workspaces, fetch-workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest active-workspaces-test
  (testing "merges config def data"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [wm/*wm* (OneWorkspaceWM.)
         clawe.config/*config*
         (atom
           {:workspace/defs
            {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})]
        (let [wsps (wm/active-workspaces)]
          (is (valid [:sequential workspace/schema] wsps))
          (is (= test-wsp-title
                 (-> wsps first :workspace/title)))
          (is (= expected-dir
                 (-> wsps first :workspace/directory))))))))

(deftest fetch-workspace-test
  (testing "active-workspaces list matches fetch-workspace output"
    (let [wsps (->> (wm/active-workspaces) (take 3))]
      (is (count wsps))
      (doall
        (for [wsp wsps]
          (do
            (testing "fetch with whole wsp"
              (let [fetched (wm/fetch-workspace wsp)]
                (is (= fetched wsp))))
            (testing "fetch with workspace title"
              (let [fetched (wm/fetch-workspace (:workspace/title wsp))]
                (is (= fetched wsp)))))))))

  (testing "similar match, :include-clients"
    (let [wsps (->> (wm/active-workspaces {:include-clients true})
                    (take 3))]
      (doall
        (for [wsp wsps]
          (do
            (testing "fetch with whole wsp"
              (let [fetched (wm/fetch-workspace
                              {:include-clients true}
                              wsp)]
                (is (= fetched wsp))))
            (testing "fetch with workspace title"
              (let [fetched (wm/fetch-workspace
                              {:include-clients true}
                              (:workspace/title wsp))]
                (is (= fetched  wsp))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; active-clients, fetch-client, current-client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def test-client-title "my-client")
(def test-app-name "some-app")

(def test-client {:client/window-title test-client-title
                  :client/app-name     test-app-name
                  :client/focused      nil})

(defrecord OneClientWM []
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] [{:workspace/clients [test-client]}])
  (-active-workspaces [_this _opts] [{:workspace/clients [test-client]}])
  (-active-clients [_this _opts] [test-client]))

(deftest client-def-merging-test
  (testing "merges config def data"
    (let [test-client-key "my-client-key"]
      (sys/with-system
        [wm/*wm* (OneClientWM.)
         clawe.config/*config*
         ;; matching on test-app-name
         (atom
           {:client/defs {test-client-key {:client/app-names [test-app-name]
                                           :match/skip-title true}}})]

        (let [clients (wm/active-clients)]
          (is (valid [:sequential client/schema] clients))
          (is (= test-client-title (-> clients first :client/window-title)))
          ;; make sure the config vals got merged in
          (is (= test-client-key (-> clients first :client/key)))
          (is (= [test-app-name] (-> clients first :client/app-names))))))))

(deftest client-def-merging-with-match-opts-test
  (testing "respects client-def :match/ options"
    (let [hard-match-key "hard-match-key"
          soft-match-key "soft-match-key"]
      (sys/with-system
        [wm/*wm* (OneClientWM.)
         clawe.config/*config*
         (atom
           {:client/defs
            ;; hard-match doesn't match any clients
            {hard-match-key {:misc/data           "specific"
                             :client/app-names    [test-app-name]
                             :client/window-title (str test-client-title "-hard-match")}
             ;; soft-match matches our client b/c of :match/soft-title
             soft-match-key {:misc/data           "vague"
                             :client/app-names    [test-app-name]
                             :client/window-title (str test-client-title "-soft-match")
                             :match/soft-title    true}}})]

        (let [clients (wm/active-clients)
              client  (first clients)
              assert-on
              (fn [c]
                (is (valid client/schema c))
                (is (= (str test-client-title "-soft-match") (-> c :client/window-title)))
                (is (= soft-match-key (-> c :client/key)))
                (is (= "vague" (-> c :misc/data))))]

          (testing "wm/active-clients"
            (assert-on client))
          (testing "wm/fetch-client"
            (assert-on (wm/fetch-client client)))
          (testing "wm/current-workspaces"
            (assert-on (->> (wm/current-workspace) :workspace/clients first)))
          (testing "wm/active-workspaces"
            (assert-on (->> (wm/active-workspaces) first :workspace/clients first))))))))

(deftest fetch-client-test
  (testing "active-clients and fetch-clients match"
    (let [clients (wm/active-clients)]
      (doall
        (for [c clients]
          (let [fetched (wm/fetch-client c)]
            (is (= fetched c))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace-defs, client-defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest workspace-defs-test
  (testing "re-uses the map key as the title."
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [clawe.config/*config*
         (atom
           {:workspace/defs
            {test-wsp-title {:workspace/directory "~/russmatney/blah"}}})]
        (is (= test-wsp-title
               (-> (wm/workspace-defs) first :workspace/title)))
        (is (= expected-dir
               (-> (wm/workspace-defs) first :workspace/directory)))))))

(deftest client-defs-test
  (testing "uses the key as the client/key"
    (let [key     "journal"
          j-title "journal-title"]
      (sys/with-system
        [clawe.config/*config*
         (atom
           {:client/defs
            {key {:client/window-title j-title}}})]
        (is (= key (-> (wm/client-defs) first :client/key)))
        (is (= j-title (-> (wm/client-defs) first :client/window-title)))))))
