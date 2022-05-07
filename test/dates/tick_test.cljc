(ns dates.tick-test
  (:require
   [clojure.test :refer [deftest is]]
   [dates.tick :as sut]))

(deftest parse-time-string-test
  (let [cases
        {"2022-02-26_15:47:52-0500"
         #time/zoned-date-time "2022-02-26T15:47:52-05:00"

         "2022-04-24_9.23.32 PM"
         #time/zoned-date-time "2022-04-24T21:23:32-04:00[America/New_York]"

         "2022-04-24 Sun"
         #time/zoned-date-time "2022-04-24T00:00:00-04:00[America/New_York]"

         "Fri, 10 Dec 2021 00:35:56 -0500"
         #time/zoned-date-time "2021-12-10T00:35:56-05:00"

         "Fri, 4 Feb 2022 15:38:14 -0500"
         #time/zoned-date-time "2022-02-04T15:38:14-05:00"

         "2021-12-30T17:52:12Z"
         #time/zoned-date-time "2021-12-30T17:52:12Z"

         "Fri May 6 12:33:33 2022 -0400"
         #time/zoned-date-time "2022-05-06T12:33:33-04:00"

         "2022-04-28T23:47:51"
         #time/zoned-date-time "2022-04-28T23:47:51-04:00[America/New_York]"}]
    (->> cases
         (map (fn [[in exp]]
                (is (= exp (sut/parse-time-string in)))))
         doall)))
