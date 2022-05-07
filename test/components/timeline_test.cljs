(ns components.timeline-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [components.timeline :as sut]
   [tick.core :as t]))

(deftest timestamps-date-data-test
  (is (= (sut/timestamps-date-data
           [#time/zoned-date-time "2022-02-26T15:47:52-05:00"
            #time/zoned-date-time "2022-04-06T15:47:52-05:00"
            #time/zoned-date-time "2021-12-26T15:47:52-05:00"])

         {:timestamps
          [#time/zoned-date-time "2022-02-26T15:47:52-05:00"
           #time/zoned-date-time "2022-04-06T15:47:52-05:00"
           #time/zoned-date-time "2021-12-26T15:47:52-05:00"]
          :newest
          #time/zoned-date-time "2022-04-06T15:47:52-05:00"
          :oldest
          #time/zoned-date-time "2022-03-24T15:47:52-05:00"
          :all-dates
          '(#time/zoned-date-time "2022-03-24T15:47:52-05:00"
             #time/zoned-date-time "2022-03-25T15:47:52-05:00"
             #time/zoned-date-time "2022-03-26T15:47:52-05:00"
             #time/zoned-date-time "2022-03-27T15:47:52-05:00"
             #time/zoned-date-time "2022-03-28T15:47:52-05:00"
             #time/zoned-date-time "2022-03-29T15:47:52-05:00"
             #time/zoned-date-time "2022-03-30T15:47:52-05:00"
             #time/zoned-date-time "2022-03-31T15:47:52-05:00"
             #time/zoned-date-time "2022-04-01T15:47:52-05:00"
             #time/zoned-date-time "2022-04-02T15:47:52-05:00"
             #time/zoned-date-time "2022-04-03T15:47:52-05:00"
             #time/zoned-date-time "2022-04-04T15:47:52-05:00"
             #time/zoned-date-time "2022-04-05T15:47:52-05:00"
             #time/zoned-date-time "2022-04-06T15:47:52-05:00")
          :all-months
          (#time/zoned-date-time "2022-03-24T15:47:52-05:00")
          :dates-by-month
          {#time/month "MARCH"
           [#time/zoned-date-time "2022-03-24T15:47:52-05:00"
            #time/zoned-date-time "2022-03-25T15:47:52-05:00"
            #time/zoned-date-time "2022-03-26T15:47:52-05:00"
            #time/zoned-date-time "2022-03-27T15:47:52-05:00"
            #time/zoned-date-time "2022-03-28T15:47:52-05:00"
            #time/zoned-date-time "2022-03-29T15:47:52-05:00"
            #time/zoned-date-time "2022-03-30T15:47:52-05:00"
            #time/zoned-date-time "2022-03-31T15:47:52-05:00"]
           #time/month "APRIL"
           [#time/zoned-date-time "2022-04-01T15:47:52-05:00"
            #time/zoned-date-time "2022-04-02T15:47:52-05:00"
            #time/zoned-date-time "2022-04-03T15:47:52-05:00"
            #time/zoned-date-time "2022-04-04T15:47:52-05:00"
            #time/zoned-date-time "2022-04-05T15:47:52-05:00"
            #time/zoned-date-time "2022-04-06T15:47:52-05:00"]}})))

(comment
  (def --dates
    [(t/zoned-date-time)
     (t/<< (t/zoned-date-time) (t/new-period 1 :months))
     (t/<< (t/zoned-date-time) (t/new-period 1 :days))])

  (sut/timestamps-date-data --dates)

  (->> --dates sut/timestamps-date-data :dates-by-month)

  (->> --dates
       sut/timestamps-date-data
       :all-dates
       (map t/zoned-date-time)
       (map (fn [date-month]
              (t/format (t/formatter "EE d") date-month))))

  (->> --dates
       sut/timestamps-date-data
       :all-months
       (map t/zoned-date-time)
       (map (fn [date-month]
              (t/format (t/formatter "MMM") date-month)))))
