(ns api.events
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.screenshots :as c.screenshots]
   [tick.core :as t]

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

(defn recent-events []
  (let [midnight (->
                   (t/tomorrow)
                   (t/at (t/midnight))
                   (t/zoned-date-time))
        ->sorted-list
        (fn [get-items]
          (->> (get-items)
               (map ->event)
               (filter :event/timestamp)
               (filter (comp
                         #(t/< % midnight)
                         :event/timestamp))
               (sort-by :event/timestamp t/>)
               (take 45)))]
    (->> (concat (->sorted-list c.screenshots/all-screenshots)
                 (->sorted-list todos/recent-org-items)
                 (->sorted-list c.git/list-db-commits))
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
