(ns defthing.defworkspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [defthing.defworkspace :as sut]
   [ralphie.zsh :as zsh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; test-workspace storage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- assert-test-workspace [wsp test-val]
  (is (= "test-workspace" (-> wsp :name)))
  (is (= test-val (-> wsp :test-val))))


(defn- assert-db-workspace-fetches
  [workspace test-val]
  (testing "get-db-workspace"
    (-> (sut/get-db-workspace workspace)
        (assert-test-workspace test-val))

    (testing "with a string"
      (-> (sut/get-db-workspace (:name workspace))
          (assert-test-workspace test-val))))

  (testing "latest-db-workspaces"
    (let [match (->> (sut/latest-db-workspaces)
                     (filter (comp #{"test-workspace"} :name))
                     first)]
      (assert-test-workspace match test-val))))


(defn- assert-in-memory-workspace-fetches
  [workspace test-val]
  (testing "get-workspace"
    (-> (sut/get-workspace workspace)
        (assert-test-workspace test-val))

    (testing "with a string"
      (-> (sut/get-workspace (:name workspace))
          (assert-test-workspace test-val))))

  (testing "list-workspaces"
    (let [match (->> (sut/list-workspaces)
                     (filter (comp #{"test-workspace"} :name))
                     first)]
      (assert-test-workspace match test-val))))


(deftest defworkspaces-memory-and-storage-test
  (let [my-random-uuid (random-uuid)]
    (testing "defworkspace macro"
      (sut/defworkspace test-workspace {:test-val my-random-uuid})

      (testing "in-memory fetches"
        (assert-in-memory-workspace-fetches
          test-workspace my-random-uuid))

      ;; syncing `all` (no params) here is problemmatic for testing,
      ;; b/c it'll overwrite the :test-val with whatever's in the db
      (testing "sync-workspaces-to-db"
        (let [res (sut/sync-workspaces-to-db test-workspace)]
          (is res)
          ;; some val, at least
          (is (-> res :datoms-transacted))))

      (testing "db fetches"
        (assert-db-workspace-fetches test-workspace my-random-uuid))

      (testing "supports overwrites via the db (single map)"
        (let [new-val "some-new-val"]
          (sut/sync-workspaces-to-db
            (assoc test-workspace :test-val new-val))

          (testing "db fetches"
            (assert-db-workspace-fetches test-workspace new-val)))

        (testing "supports overwrites via the db (list of maps)"
          (let [new-val "some-other-val"]
            (sut/sync-workspaces-to-db
              [(assoc test-workspace :test-val new-val)])

            (testing "db fetches"
              (assert-db-workspace-fetches test-workspace new-val))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo workspace install and storage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def home-dir (zsh/expand "~"))

(defn- assert-repo-path [wsp path title]
  (let [dir (str home-dir "/" path)]
    (is wsp)
    (is (= path (-> wsp :repo/short-path)))
    (is (= title (-> wsp :workspace/title)))
    (is (= dir (-> wsp :workspace/directory)))
    (is (= dir (-> wsp :git/repo)))))

(deftest install-repo-workspaces-test
  (let [repo-paths
        {"clawe-test"    "russmatney/clawe-test"
         "wing-test"     "/home/russ/teknql/wing-test"
         "wing-test-two" "/home/russ/teknql/wing-test-two"}]
    (let [res (sut/install-repo-workspaces (vals repo-paths))]
      (is res)
      ;; some datoms, at least
      (is (-> res :datoms-transacted)))

    (testing "installed wsps are listed from the db"
      (let [wsps     (sut/latest-db-workspaces)
            find-wsp (fn [n]
                       (->> wsps
                            (filter (comp #{n} :name))
                            first))]
        (for [[n path] repo-paths]
          (let [wsp (find-wsp n)]
            (assert-repo-path wsp path n)))))))
