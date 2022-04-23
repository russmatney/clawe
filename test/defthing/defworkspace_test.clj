(ns defthing.defworkspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [defthing.defworkspace :as sut]))

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
        (assert-in-memory-workspace-fetches test-workspace my-random-uuid))

      (sut/sync-workspaces-to-db)
      (testing "db fetches"
        (assert-db-workspace-fetches test-workspace my-random-uuid))

      (testing "supports overwrites via the db"
        (let [new-id-val "some-new-val"]
          (sut/sync-workspaces-to-db
            (assoc test-workspace :test-val new-id-val))

          (testing "db fetches"
            (assert-db-workspace-fetches test-workspace new-id-val)))))))

;; TODO test install-repo-workspaces
