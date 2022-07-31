(ns api.commits
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [tick.core :as t]

   [git.core :as git]
   [dates.tick :as dt]
   [item.core :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->event [item]
  (if-let [time-str (item/->time-string item)]
    (if-let [parsed (dt/parse-time-string time-str)]
      (cond-> item true (assoc :event/timestamp parsed))
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

(defn recent-commits []
  (->> (->sorted-list git/list-db-commits)
       (sort-by :event/timestamp t/>)
       (into [])))

(defsys *commits-stream*
  :start (s/stream)
  :stop (s/close! *commits-stream*))

(defn re-push-commits []
  (let [evts (recent-commits)]
    (println "pushing to commits stream!" (count evts))
    (s/put! *commits-stream* evts)))

(comment
  (re-push-commits))

