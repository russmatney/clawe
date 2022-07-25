(ns defthing.db-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [defthing.db :as sut]
   [dates.tick :as dates.tick]
   [tick.core :as t]))

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


(deftest transacting-dates
  (testing "dates can be written to and read from the db"
    (let [rn           (t/inst)
          my-test-uuid (random-uuid)
          ent          {:test/id my-test-uuid :time/rn rn}
          res          (sut/transact [ent])]
      (is (= res {:datoms-transacted 2}))

      (let [fetched (sut/query '[:find [(pull ?e [*])]
                                 :in $ ?id
                                 :where [?e :test/id ?id]])]
        (is (= fetched ent))
        (is (= (:time/rn fetched) rn))))
    ))

(comment
  (println "hi")

  (sut/dump)
  )
