(ns dates.tick
  (:require
   [ralphie.notify :as notify]
   [clojure.string :as string]
   [tick.core :as t]))

;; TODO unit tests
(defn parse-time-string
  [time-string]
  (when (and time-string (string? time-string) (seq (string/trim time-string)))
    (let [parse-attempts
          [#(t/parse-zoned-date-time
              % (t/formatter "yyyy-MM-dd_HH:mm:ssZ"))
           #(t/zoned-date-time
              (t/parse-date-time
                % (t/formatter "yyyy-MM-dd_h.mm.ss a")))
           #(t/parse-zoned-date-time
              % (t/formatter "E, d MMM yyyy HH:mm:ss Z"))
           #(t/zoned-date-time
              (t/parse-date-time
                % (t/formatter "yyyy-MM-dd-HHmmss")))
           #(t/zoned-date-time
              (t/parse-date-time
                % (t/formatter "yyyy-MM-dd E HH:mm")))
           #(->
              (t/parse-date % (t/formatter "yyyy-MM-dd E"))
              (t/at (t/midnight))
              (t/zoned-date-time))]
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
  ((some-fn :screenshot/time-string :y)
   {:screenshot/time-string "x"
    :y                      "z"})

  (t/parse-zoned-date-time
    "Fri, 10 Dec 2021 00:35:56 -0500"
    (t/formatter "E, d MMM yyyy HH:mm:ss Z"))

  (->
    (t/parse-date
      "2022-04-24 Sun"
      (t/formatter "yyyy-MM-dd E"))
    (t/at (t/midnight))
    (t/zoned-date-time)
    )

  (parse-time-string "x")
  (parse-time-string "2022-02-26_15:47:52-0500")
  (parse-time-string "2022-04-24_9.23.32 PM")
  (parse-time-string "2022-04-24 Fri")
  (parse-time-string "Fri, 10 Dec 2021 00:35:56 -0500")
  (parse-time-string "Fri, 4 Feb 2022 15:38:14 -0500")
  )
