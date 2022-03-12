(ns doctor.api.events
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.screenshots :as c.screenshots]
   [tick.core :as t]
   [ralphie.notify :as notify]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-screenshot-time-string [x]
  (try
    (-> x
        :screenshot/time-string
        (t/parse-zoned-date-time (t/formatter "yyyy-MM-dd_HH:mm:ssZ")))
    (catch Exception e e
           (notify/notify "Error parsing screenshot time string" (-> x :screenshot/time-string))
           x)))

(comment
  (t/parse-zoned-date-time
    "2022-02-26_15:47:52-0500"
    (t/formatter "yyyy-MM-dd_HH:mm:ssZ"))

  (t/offset-date-time "2022-02-26T15:47:52-0500"))

(defn ->event [x]
  (cond
    (:screenshot/time-string x)
    (-> x
        (assoc :event/timestamp (parse-screenshot-time-string x)))

    :else x
    ))

(defn recent-events []
  (let [scrs (c.screenshots/all-screenshots)
        ]
    (->> (concat (->> scrs (take 30))
                 )
         (map ->event) (into []))))

(comment
  (recent-events))

(defsys *events-stream*
  :start (s/stream)
  :stop (s/close! *events-stream*))

(defn re-push-events []
  (let [evts (recent-events)]
    (println "pushing to events stream!" (count evts))
    (s/put! *events-stream* evts)))

(comment
  (re-push-events))
