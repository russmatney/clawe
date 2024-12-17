(ns dates.tick
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [util :as util]))

(def date-formats-with-zone
  [:iso-zoned-date-time
   :iso-offset-date-time
   "yyyy-MM-dd_HH:mm:ssZ"
   "E, d MMM yyyy HH:mm:ss Z"
   "E MMM d HH:mm:ss yyyy Z"])

(def datetime-formats-without-zone
  ["yyyy-MM-dd_h.mm.ss a"
   "yyyy-MM-dd_h.mm.ss a"
   "yyyy-MM-dd-HHmmss"
   "yyyy-MM-ddTHH:mm:ss"
   "yyyy-MM-dd HH:mm:ss"
   "yyyy-MM-dd_HH.mm.ss"
   "yyyy-MM-dd_HH-mm"
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
  (cond
    (and time-string
         (or
           (t/instant? time-string)
           (t/date? time-string)
           (t/date-time? time-string)
           (t/zoned-date-time? time-string)))
    ;; support passing already parsed time through
    time-string

    (inst? time-string) (t/zoned-date-time time-string)

    :else
    (when (and time-string
               (or (and (string? time-string) (seq (string/trim time-string)))
                   (int? time-string)))
      (if-let [time (cond-> time-string
                      (string? time-string) string/trim
                      true                  ((apply some-fn wrapped-parse-attempts)))]
        time
        (do
          #?(:clj (println "Error parsing time string" time-string)
             :cljs (println "Error parsing time string" time-string))
          nil)))))

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
  (parse-time-string (parse-time-string 1650486902722))

  (parse-time-string "20220722:105834")

  (parse-time-string "x")
  (parse-time-string "2022-02-26_15:47:52-0500")
  (parse-time-string "2022-04-24_9.23.32 PM")
  (parse-time-string "2022-04-24 Sun")
  (parse-time-string "Fri, 10 Dec 2021 00:35:56 -0500")
  (parse-time-string "Fri, 4 Feb 2022 15:38:14 -0500")
  (parse-time-string "2021-12-30T17:52:12Z")
  (parse-time-string "2023-04-04T21:11:15Z")
  (parse-time-string "2023-02-28_23.23.07")
  (parse-time-string "2023-03-17_19.26.15 ")
  (parse-time-string "2024-12-17_11.18.37 AM")
  (parse-time-string "2023-04-23_11.12.08 PM 1")
  (parse-time-string "2023-04-23_11.12.08 PM 4")

  (parse-time-string "2024-03-06_16-22")
  (parse-time-string "hex-zoomed-in_2024-03-06_16-22")

  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; now
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn now []
  (t/zoned-date-time))

(defn add-tz
  "TODO remove this, as it's apparently just a t/zoned-date-time call."
  [inst]
  (t/zoned-date-time inst)
  #_(if (t/instant inst)
      (t/zoned-date-time)
      inst))

(comment
  (t/instant? (t/inst))
  (add-tz (t/instant (t/inst)))
  (add-tz (t/instant (t/zoned-date-time))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ago
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn duration-since
  ([inst] (duration-since inst nil))
  ([inst end]
   (when inst
     (t/duration
       {:tick/beginning (parse-time-string inst)
        :tick/end       (or (and end (parse-time-string end))
                            (t/zoned-date-time))}))))

(comment
  (duration-since (t/at (t/yesterday)
                        (t/midnight))
                  (t/now)))

(defn millis-since
  [start-inst]
  (t/millis (t/duration {:tick/beginning start-inst :tick/end (t/now)})))

(defn human-time-since
  ([inst] (human-time-since inst nil))
  ([inst end]
   (when inst
     (let [end     (or end (t/zoned-date-time))
           since   (duration-since inst end)
           days    (t/days since)
           hours   (t/hours since)
           mins    (t/minutes since)
           seconds (t/seconds since)]
       (cond
         (> days 1)    (str days " days")
         (= days 1)    (str days "d")
         (> hours 1)   (let [mins (- mins (* hours 60))]
                         (str hours ":" (util/zp mins 2) "m"))
         (= hours 1)   (let [mins (- mins 60)]
                         (str hours ":" (util/zp mins 2) "m"))
         (> mins 1)    (str mins "m")
         (= mins 1)    (let [secs (- seconds 60)]
                         (str mins ":" (util/zp secs 2) "s"))
         (> seconds 1) (str seconds " secs")
         (= seconds 1) (str seconds "s"))))))

(comment
  (util/zp 5 2)
  (duration-since (parse-time-string "2022-02-26_15:47:52-0500")))

(defn an-x-ago-ms
  "Returns duration-ago as milliseconds.
  NOTE only useful for per-day units, not less than 1 day.
  "
  [duration]
  (-> (t/today)
      (t/at (t/midnight))
      (t/<< duration)
      t/inst
      inst-ms))

;; TODO support and update note above
;; (defn an-hour-ago-ms
;;   "Returns an-hour-ago as milliseconds."
;;   []
;;   (an-x-ago-ms (t/new-duration 1 :hours)))

;; (comment
;;   (an-hour-ago-ms))

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

(defn newer [a b]
  (let [a (when a (parse-time-string a))
        b (when b (parse-time-string b))]
    (cond (and a b) (t/> a b)
          a         true
          :else     nil)))

(defn older [a b]
  (let [a (when a (parse-time-string a))
        b (when b (parse-time-string b))]
    (cond (and a b) (t/< a b)
          a         true
          :else     nil)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sort
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -sort [a b comp]
  (let [a (when a (parse-time-string a))
        b (when b (parse-time-string b))]
    (cond (and a b) (comp a b)
          a         true
          :else     false)))

(defn sort-chrono [a b] (-sort a b t/<))
(defn sort-latest-first [a b] (-sort a b t/>))
