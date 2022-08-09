(ns dates.tick
  (:require
   #?@(:clj [[ralphie.notify :as notify]])
   [clojure.string :as string]
   [tick.core :as t]))

(def date-formats-with-zone
  [:iso-zoned-date-time
   :iso-offset-date-time
   "yyyy-MM-dd_HH:mm:ssZ"
   "E, d MMM yyyy HH:mm:ss Z"
   "E MMM d HH:mm:ss yyyy Z"])

(def datetime-formats-without-zone
  ["yyyy-MM-dd_h.mm.ss a"
   "yyyy-MM-dd-HHmmss"
   "yyyy-MM-ddTHH:mm:ss"
   "yyyy-MM-dd HH:mm:ss"
   "yyyy-MM-dd E HH:mm"
   "yyyyMMdd:HHmmss"
   :iso-local-date-time])

;; :iso-local-time
;; :iso-instant

(def date-formats
  ["yyyy-MM-dd E"
   :iso-local-date])

(def parse-attempts
  (concat
    (->> date-formats-with-zone
         (map (fn [f]
                #(t/parse-zoned-date-time % (t/formatter f)))))
    (->> datetime-formats-without-zone
         (map (fn [f]
                #(-> (t/parse-date-time % (t/formatter f))
                     t/zoned-date-time))))
    (->> date-formats
         (map (fn [f]
                #(-> (t/parse-date % (t/formatter f))
                     (t/at (t/midnight))
                     (t/zoned-date-time)))))
    (list (fn [input]
            (when (int? input)
              (->
                (t/instant input)
                (t/zoned-date-time)))))))

(def wrapped-parse-attempts
  (->> parse-attempts
       (map (fn [parse]
              #(try
                 (parse %)
                 (catch #?(:clj Exception :cljs js/Error) e e nil))))))

(defn parse-time-string
  [time-string]
  (when (and time-string
             (or (and (string? time-string) (seq (string/trim time-string)))
                 (int? time-string)))
    (if-let [time ((apply some-fn wrapped-parse-attempts) time-string)]
      time
      (do
        #?(:clj (notify/notify "Error parsing time string" time-string)
           :cljs (println "Error parsing time string" time-string))
        nil))))

(comment
  (t/parse-zoned-date-time
    "Fri, 10 Dec 2021 00:35:56 -0500"
    (t/formatter "E, d MMM yyyy HH:mm:ss Z"))

  (->
    (t/parse-date
      "2022-04-24 Sun"
      (t/formatter "yyyy-MM-dd E"))
    (t/at (t/midnight))
    (t/zoned-date-time))

  (t/parse-zoned-date-time
    "2021-12-30T17:52:12Z"
    (t/formatter :iso-zoned-date-time))

  (t/instant 1650486902722)
  (parse-time-string 1650486902722)

  (parse-time-string "20220722:105834")

  (parse-time-string "x")
  (parse-time-string "2022-02-26_15:47:52-0500")
  (parse-time-string "2022-04-24_9.23.32 PM")
  (parse-time-string "2022-04-24 Sun")
  (parse-time-string "Fri, 10 Dec 2021 00:35:56 -0500")
  (parse-time-string "Fri, 4 Feb 2022 15:38:14 -0500")
  (parse-time-string "2021-12-30T17:52:12Z"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; now
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn now []
  (t/zoned-date-time))

(defn add-tz [inst]
  ;; TODO this is not really right...
  (if (t/instant inst)
    (t/in inst "America/New_York")
    inst))

(comment
  (t/instant? (t/inst))
  (add-tz (t/instant (t/inst)))
  (add-tz (t/instant (t/zoned-date-time))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ago
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn an-x-ago-ms
  "Returns duration-ago as milliseconds."
  [duration]
  (-> (t/today)
      (t/at (t/midnight))
      (t/<< duration)
      t/inst
      inst-ms))

(defn a-week-ago-ms
  "Returns a-week-ago as milliseconds."
  []
  (an-x-ago-ms (t/new-duration 7 :days)))

(defn a-month-ago-ms
  "Returns a-month-ago as milliseconds."
  []
  (an-x-ago-ms (t/new-duration 31 :days)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; days
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn days
  "Returns dates for the last n days, including today."
  [n]
  (t/range
    (-> (t/today) (t/<< (t/new-period (dec n) :days)))
    (t/tomorrow)))

(comment
  (count
    (t/range
      (-> (t/today) (t/<< (t/new-period 14 :days)))
      (t/today)
      (t/new-period 1 :days)
      ))

  (days 14))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; months
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn months
  ([] (months 3))
  ([n]
   (->>
     (range n)
     (map (fn [x] (t/<< (t/today) (t/new-period x :months))))
     (map (fn [t] (t/format (t/formatter "yyyy-MM") t))))))

(comment
  (months)
  (months 5))
