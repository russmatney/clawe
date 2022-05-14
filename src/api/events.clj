(ns api.events
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.screenshots :as c.screenshots]
   [tick.core :as t]

   [chess.db]
   [dates.tick :as dt]
   [api.todos :as todos]
   [clawe.git :as c.git]
   [item.core :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->event [item]
  (if-let [time-str (item/->time-string item)]
    (if-let [parsed (dt/parse-time-string time-str)]
      (-> item (assoc :event/timestamp parsed))
      item)
    item))

(defn ->sorted-list
  ([get-items-f] (->sorted-list {} get-items-f))
  ([{:keys [take-n]} get-items-f]
   (let [take-n   (or take-n 45)
         midnight (-> (t/tomorrow) (t/at (t/midnight)) (t/zoned-date-time))]
     (->> (get-items-f)
          (map ->event)
          (filter :event/timestamp)
          (filter (comp #(t/< % midnight) :event/timestamp))
          (sort-by :event/timestamp t/>)
          (take take-n)))))

(comment
  (->>
    (chess.db/fetch-db-games)
    (take 3)
    (map item/->time-string))

  (->>
    (->sorted-list chess.db/fetch-db-games)
    (take 3)))

(defn recent-events []
  (->> (concat (->sorted-list c.screenshots/all-screenshots)
               (->sorted-list todos/recent-org-items)
               (->sorted-list c.git/list-db-commits)
               (->sorted-list {:take-n 50} chess.db/fetch-db-games))
       (sort-by :event/timestamp t/>)
       (into [])))

(comment
  (count
    (recent-events))

  (take 2 (chess.db/fetch-db-games))

  (count
    (->sorted-list {:take-n 50} chess.db/fetch-db-games))

  (->>
    (recent-events)
    (filter :lichess.game/id)
    (take 20))
  )

(defsys *events-stream*
  :start (s/stream)
  :stop (s/close! *events-stream*))

(defn re-push-events []
  (let [evts (recent-events)]
    (println "pushing to events stream!" (count evts))
    (s/put! *events-stream* evts)))

(comment
  (re-push-events))
