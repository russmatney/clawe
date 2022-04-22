(ns defthing.defworkspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [defthing.defworkspace :as sut]))

(deftest defworkspaces-memory-and-storage-test
  (let [my-random-uuid (random-uuid)]
    (testing "defworkspace macro"
      (sut/defworkspace test-workspace
        {:test-val my-random-uuid})

      (testing "get-workspace"
        (let [fetched (sut/get-workspace test-workspace)]
          (is (= "test-workspace" (-> fetched :name)))
          (is (= my-random-uuid (-> fetched :test-val))))

        (testing "with a string"
          (let [fetched (sut/get-workspace (:name test-workspace))]
            (is (= "test-workspace" (-> fetched :name)))
            (is (= my-random-uuid (-> fetched :test-val))))))

      (testing "list-workspaces"
        (let [latest (sut/list-workspaces)
              match  (->> latest
                          (filter (comp #{"test-workspace"} :name))
                          first)]
          (is (= my-random-uuid
                 (-> match :test-val)))))

      ;; adds all required defworkspaced files to the db
      (sut/sync-workspaces-to-db)

      (testing "get-db-workspace"
        (let [fetched (sut/get-db-workspace test-workspace)]
          (is (= "test-workspace" (-> fetched :name)))
          (is (= my-random-uuid (-> fetched :test-val))))
        (testing "with a string"
          (let [fetched (sut/get-db-workspace (:name test-workspace))]
            (is (= "test-workspace" (-> fetched :name)))
            (is (= my-random-uuid (-> fetched :test-val))))))

      (testing "latest-db-workspaces"
        (let [match (->> (sut/latest-db-workspaces)
                         (filter (comp #{"test-workspace"} :name))
                         first)]
          (is (= "test-workspace" (-> match :name)))
          (is (= my-random-uuid (-> match :test-val)))))

      (testing "supports overwrites (the db's latest)"
        ;; overwrite :test-val with transaction/update fn
        ;; then test the db fetches
        ))))

(comment
  (defthing.db/dump)
  )

;; TODO test install-repo-workspaces
