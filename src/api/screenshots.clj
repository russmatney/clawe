(ns api.screenshots
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [screenshots.core :as screenshots]
   [db.core :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-screenshots []
  (let [all (screenshots/all-screenshots)]
    (->> all (take 30) (into []))))

(defsys *screenshots-stream*
  :start (s/stream)
  :stop (s/close! *screenshots-stream*))

(defn update-screenshots []
  (println "pushing to screenshots stream (updating screenshots)!")
  (s/put! *screenshots-stream* (active-screenshots)))

(comment
  (update-screenshots))

(defn ingest-screenshots []
  (->> (screenshots/all-screenshots)
       (take 30)
       (db/transact)))
