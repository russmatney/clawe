(ns api.focus
  (:require [garden.core :as garden]
            [org-crud.core :as org-crud]
            [clojure.set :as set]
            [systemic.core :as sys :refer [defsys]]
            [manifold.stream :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; domain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todays-org-item []
  (org-crud/path->nested-item (garden/daily-path)))

(defn todays-goals
  "Top level items tagged 'goal' or 'goals'"
  []
  (->>
    (todays-org-item)
    :org/items
    (filter (comp seq #(set/intersection
                         #{"goal" "goals"} %) :org/tags))
    (mapcat org-crud/nested-item->flattened-items)))

(comment
  (todays-goals))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; infra
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-focus-data []
  {:todos (todays-goals)})

(defsys *focus-data-stream*
  :start (s/stream)
  :stop (s/close! *focus-data-stream*))

(comment
  (sys/start! `*todos-stream*))

(defn update-focus-data []
  (s/put! *focus-data-stream* (build-focus-data)))
