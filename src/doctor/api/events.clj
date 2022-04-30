(ns doctor.api.events
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.screenshots :as c.screenshots]
   [tick.core :as t]
   [ralphie.notify :as notify]
   [doctor.api.todos :as todos]
   [clawe.git :as c.git]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->time-string [x]
  ((some-fn #(when (string? %) %)
            :screenshot/time-string
            :git.commit/author-date)
   x))

(comment
  (->time-string "x")
  (->time-string {:screenshot/time-string "x"})
  (->time-string {:git.commit/author-date "x"})
  )

;; TODO move to some better namespace... datetime/core ?
;; TODO unit tests
(defn parse-time-string
  [x]
  (when-let [time-string (->time-string x)]
    (let [parse-attempts
          [#(t/parse-zoned-date-time
              % (t/formatter "yyyy-MM-dd_HH:mm:ssZ"))
           #(t/zoned-date-time
              (t/parse-date-time
                % (t/formatter "yyyy-MM-dd_h.mm.ss a")))
           #(t/parse-zoned-date-time
              % (t/formatter "E, d MMM yyyy HH:mm:ss Z"))]
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

  (parse-time-string "x")
  (parse-time-string "2022-02-26_15:47:52-0500")
  (parse-time-string "2022-04-24_9.23.32 PM")
  (parse-time-string "Fri, 10 Dec 2021 00:35:56 -0500")
  (parse-time-string "Fri, 4 Feb 2022 15:38:14 -0500")
  )

(defn ->event [x]
  (cond
    (->time-string x)
    (-> x (assoc :event/timestamp (parse-time-string x)))

    :else x))

(defn recent-events []
  (println "collecting recent events")
  (let [scrs       (c.screenshots/all-screenshots)
        org-todos  (todos/build-org-todos)
        db-commits (c.git/list-db-commits)]
    (->> (concat (->> scrs
                      (map ->event)
                      (filter :event/timestamp)
                      (take 30))
                 ;; (->> org-todos (take 30))
                 (->> db-commits
                      (map ->event)
                      ;; ugh, this pulls all of them...
                      (filter :event/timestamp)
                      (sort-by :event/timestamp t/>)
                      (take 50)))
         (sort-by :event/timestamp t/>)
         (into []))))

(comment
  (count
    (recent-events)))

(defsys *events-stream*
  :start (s/stream)
  :stop (s/close! *events-stream*))

(defn re-push-events []
  (let [evts (recent-events)]
    (println "pushing to events stream!" (count evts))
    (s/put! *events-stream* evts)))

(comment
  (re-push-events))
