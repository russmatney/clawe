(ns doctor.api.events
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.screenshots :as c.screenshots]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recent-events []
  (let [all (c.screenshots/all-screenshots)]
    (->> all (take 30) (into []))))

(defsys *events-stream*
  :start (s/stream)
  :stop (s/close! *events-stream*))

(defn re-push-events []
  (println "pushing to events stream (updating events)!")
  (s/put! *events-stream* (recent-events)))

(comment
  (re-push-events))
