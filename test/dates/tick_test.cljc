(ns dates.tick-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [dates.tick :as sut]
   [tick.core :as t]))

(defn ->zoned-date-time [s]
  (t/parse-zoned-date-time s (t/formatter :iso-zoned-date-time)))

(defn ->zdt-with-local [s]
  (->
    (t/parse-date-time s (t/formatter :iso-local-date-time))
    (t/zoned-date-time)))

(deftest parse-time-string-test
  (testing "basic cases"
    (let [cases
          {"2022-02-26_15:47:52-0500"
           (->zoned-date-time "2022-02-26T15:47:52-05:00")

           "Fri, 10 Dec 2021 00:35:56 -0500"
           (->zoned-date-time "2021-12-10T00:35:56-05:00")

           "Fri, 4 Feb 2022 15:38:14 -0500"
           (->zoned-date-time "2022-02-04T15:38:14-05:00")

           "2021-12-30T17:52:12Z"
           (->zoned-date-time "2021-12-30T17:52:12Z")

           "Fri May 6 12:33:33 2022 -0400"
           (->zoned-date-time "2022-05-06T12:33:33-04:00")}]
      (->> cases
           (map (fn [[in exp]]
                  (is (= exp (sut/parse-time-string in)))))
           doall)))

  (testing "timezone cases"
    (let [cases
          {"2022-04-24_9.23.32 PM"
           (->zdt-with-local "2022-04-24T21:23:32")

           "2022-04-24 Sun"
           (->zdt-with-local "2022-04-24T00:00:00")

           "2022-04-28T23:47:51"
           (->zdt-with-local "2022-04-28T23:47:51")

           "2022-04-28 23:47:51"
           (->zdt-with-local "2022-04-28T23:47:51")}]
      (->> cases
           (map (fn [[in exp]]
                  (is (= exp (sut/parse-time-string in)))))
           doall)))

  (testing "handles ints"
    (let [res (sut/parse-time-string 1650486902722)]
      (is (t/zone res))
      (is (= (t/year res)
             (t/year (->zdt-with-local "2022-04-20T16:35:02.722"))))
      (is (= (t/month res)
             (t/month (->zdt-with-local "2022-04-20T16:35:02.722"))))
      (is (= (t/date res)
             (t/date (->zdt-with-local "2022-04-20T16:35:02.722")))))))
