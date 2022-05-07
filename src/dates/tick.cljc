(ns dates.tick
  (:require
   [ralphie.notify :as notify]
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
   "yyyy-MM-dd E HH:mm"
   :iso-local-date-time])

;; :iso-local-time
;; :iso-instant

(def date-formats
  ["yyyy-MM-dd E"
   :iso-local-date])

(defn parse-time-string
  [time-string]
  (when (and time-string (string? time-string) (seq (string/trim time-string)))
    (let [parse-attempts
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
                             (t/zoned-date-time))))))

          wrapped-parse-attempts
          (->> parse-attempts
               (map (fn [parse]
                      #(try
                         (parse %)
                         (catch Exception e e nil)))))]
      (if-let [time ((apply some-fn wrapped-parse-attempts) time-string)]
        time
        (do
          (notify/notify "Error parsing time string" time-string)
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

  (parse-time-string "x")
  (parse-time-string "2022-02-26_15:47:52-0500")
  (parse-time-string "2022-04-24_9.23.32 PM")
  (parse-time-string "2022-04-24 Sun")
  (parse-time-string "Fri, 10 Dec 2021 00:35:56 -0500")
  (parse-time-string "Fri, 4 Feb 2022 15:38:14 -0500")
  (parse-time-string "2021-12-30T17:52:12Z"))
