(ns api.focus
  (:require
   [garden.core :as garden]
   [garden.db :as garden.db]
   [org-crud.core :as org-crud]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; domain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn relevant-org-items []
  (->> (concat
         (garden/daily-paths 7)
         (->> (garden/last-modified-paths) (take 30)))
       (distinct)
       garden/paths->nested-garden-notes))

(defn todays-goals []
  (->> (relevant-org-items)
       (mapcat org-crud/nested-item->flattened-items)
       (filter :org/status)
       (map garden.db/merge-db-item)))

(comment
  (todays-goals))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; infra
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-focus-data []
  {:todos (todays-goals)})

(defsys ^:dynamic *focus-data-stream*
  :start (s/stream)
  :stop (s/close! *focus-data-stream*))

(comment
  (sys/start! `*focus-data-stream*))

(defn update-focus-data []
  (s/put! *focus-data-stream* (build-focus-data)))
