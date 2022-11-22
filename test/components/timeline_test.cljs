(ns components.timeline-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [components.timeline :as sut]
   [time-literals.data-readers]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [tick.core :as t]))

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

(defn ->zoned-date-time [s]
  (t/parse-zoned-date-time s (t/formatter :iso-zoned-date-time)))

(deftest timestamps-date-data-test
  (let [res (sut/timestamps-date-data
              [(->zoned-date-time "2022-02-26T15:47:52-05:00")
               (->zoned-date-time "2022-04-06T15:47:52-05:00")
               (->zoned-date-time "2021-12-26T15:47:52-05:00")])]
    (testing ":timestamps"
      (is (= (:timestamps res)
             [(->zoned-date-time "2022-02-26T15:47:52-05:00")
              (->zoned-date-time "2022-04-06T15:47:52-05:00")
              (->zoned-date-time "2021-12-26T15:47:52-05:00")])))

    (testing ":newest, :oldest"
      (is (= (:newest res)
             (->zoned-date-time "2022-04-06T15:47:52-05:00")))
      (is (= (:oldest res)
             (->zoned-date-time "2022-03-24T15:47:52-05:00"))))

    (testing ":all-dates"
      (is (= (:all-dates res)
             (list (->zoned-date-time "2022-03-24T15:47:52-05:00")
                   (->zoned-date-time "2022-03-25T15:47:52-05:00")
                   (->zoned-date-time "2022-03-26T15:47:52-05:00")
                   (->zoned-date-time "2022-03-27T15:47:52-05:00")
                   (->zoned-date-time "2022-03-28T15:47:52-05:00")
                   (->zoned-date-time "2022-03-29T15:47:52-05:00")
                   (->zoned-date-time "2022-03-30T15:47:52-05:00")
                   (->zoned-date-time "2022-03-31T15:47:52-05:00")
                   (->zoned-date-time "2022-04-01T15:47:52-05:00")
                   (->zoned-date-time "2022-04-02T15:47:52-05:00")
                   (->zoned-date-time "2022-04-03T15:47:52-05:00")
                   (->zoned-date-time "2022-04-04T15:47:52-05:00")
                   (->zoned-date-time "2022-04-05T15:47:52-05:00")
                   (->zoned-date-time "2022-04-06T15:47:52-05:00")))))

    (testing ":all-months"
      (is (= (:all-months res)
             (list (->zoned-date-time "2022-03-24T15:47:52-05:00")))))

    (testing ":dates-by-month"
      (is (= (:dates-by-month res)
             {#time/month "MARCH"
              [(->zoned-date-time "2022-03-24T15:47:52-05:00")
               (->zoned-date-time "2022-03-25T15:47:52-05:00")
               (->zoned-date-time "2022-03-26T15:47:52-05:00")
               (->zoned-date-time "2022-03-27T15:47:52-05:00")
               (->zoned-date-time "2022-03-28T15:47:52-05:00")
               (->zoned-date-time "2022-03-29T15:47:52-05:00")
               (->zoned-date-time "2022-03-30T15:47:52-05:00")
               (->zoned-date-time "2022-03-31T15:47:52-05:00")]
              #time/month "APRIL"
              [(->zoned-date-time "2022-04-01T15:47:52-05:00")
               (->zoned-date-time "2022-04-02T15:47:52-05:00")
               (->zoned-date-time "2022-04-03T15:47:52-05:00")
               (->zoned-date-time "2022-04-04T15:47:52-05:00")
               (->zoned-date-time "2022-04-05T15:47:52-05:00")
               (->zoned-date-time "2022-04-06T15:47:52-05:00")]})))))
