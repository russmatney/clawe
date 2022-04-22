(ns defthing.db-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [defthing.db :as sut]))

(deftest transact-and-query-test
  (testing "transact"
    (let [test-name "some fancy name"
          entity    {:test/name test-name}
          res       (sut/transact [entity])]
      (is (= res {:datoms-transacted 1}))

      (testing "and query"
        (let [res (sut/query '[:find [(pull ?e [*])]
                               :where [?e :test/name ?n]
                               :in $ ?n]
                             test-name)]
          (is (= test-name (-> res first :test/name)))))

      ;; TODO clean up test data?
      ;; use a deletable test db?
      )))

(comment
  (sut/dump))
