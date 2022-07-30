(ns defthing.db-test
  (:require
   [clojure.test :refer [deftest is testing] :as t]
   [defthing.db :as sut]
   [defthing.config :as defthing.config]
   ;; [dates.tick :as dates.tick]
   ;; [tick.core :as t]

   [systemic.core :as sys]))

(t/use-fixtures
  :once
  (fn [f]
    (sys/with-system
      [defthing.config/*config* {:db-path "~/russmatney/dbs/testdb"}]
      (f))))


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


;; TODO restore after impling some bb time workaround
#_(deftest transacting-date-times
    (testing "insts can be written to and read from the db"
      (let [rn           (t/inst)
            my-test-uuid (random-uuid)
            ent          {:test/id my-test-uuid :time/rn rn}
            res          (sut/transact [ent])]
        (is (= res {:datoms-transacted 2}))

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
        (is (= res {:datoms-transacted 2}))

        (let [fetched (-> (sut/query '[:find [(pull ?e [*])]
                                       :in $ ?id
                                       :where [?e :test/id ?id]]
                                     my-test-uuid)
                          first)]
          (is (= (:test/id fetched) my-test-uuid))
          (is (= (-> fetched :time/rn t/zoned-date-time) rn)))))
    )

(comment
  (println "hi")

  (sut/dump)
  )
