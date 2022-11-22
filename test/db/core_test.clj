(ns db.core-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [db.core :as sut]
   [db.config :as db.config]
   [dates.tick :as dates.tick]
   [tick.core :as t]
   [systemic.core :as sys]
   [babashka.fs :as fs]))


(use-fixtures
  :once
  (fn [f]
    (when (not (fs/exists? "dbs"))
      (fs/create-dir "dbs"))

    (sys/with-system
      [db.config/*config* {:db-path "dbs/testdb.edn"}]

      (let [p (db.config/db-path)]
        (when (fs/exists? p)
          (fs/delete p)))

      (f)

      ;; overkill
      (let [p (db.config/db-path)]
        (when (fs/exists? p)
          (fs/delete p))))))


(deftest transact-and-query-test
  (testing "transact some data"
    (let [test-name "some fancy name"
          entity    {:test/name test-name}
          res       (sut/transact [entity])]
      (is (= (count (:tx-data res)) 1))

      (testing "query the just-transacted data"
        (let [res (sut/query '[:find [(pull ?e [*])]
                               :where [?e :test/name ?n]
                               :in $ ?n]
                             test-name)]
          (is (= test-name (-> res first :test/name))))))))

(deftest transacting-date-times
  (testing "insts can be written to and read from the db"
    (let [rn           (t/inst)
          my-test-uuid (random-uuid)
          ent          {:test/id my-test-uuid :time/rn rn}
          res          (sut/transact [ent])]
      (is (= (count (:tx-data res)) 2))

      (let [fetched (-> (sut/query '[:find [(pull ?e [*])]
                                     :in $ ?id
                                     :where [?e :test/id ?id]]
                                   my-test-uuid)
                        first)]
        (is (= (:test/id fetched) my-test-uuid))
        (is (= (:time/rn fetched) rn)))))

  (testing "zoned-date-times get converted to insts"
    (let [;; convert to an instant and back so we hit the same precision
          ;; otherwise the original ZDT has milliseconds, but the db one drops those
          rn           (-> (dates.tick/now) t/inst t/zoned-date-time)
          my-test-uuid (random-uuid)
          ent          {:test/id my-test-uuid :time/rn rn}
          res          (sut/transact [ent])]
      (is (= (count (:tx-data res)) 2))

      (let [fetched (-> (sut/query '[:find [(pull ?e [*])]
                                     :in $ ?id
                                     :where [?e :test/id ?id]]
                                   my-test-uuid)
                        first)]
        (is (= (:test/id fetched) my-test-uuid))
        (is (= (-> fetched :time/rn t/zoned-date-time) rn))))))
