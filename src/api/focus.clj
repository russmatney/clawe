(ns api.focus
  (:require [garden.core :as garden]
            [org-crud.core :as org-crud]
            [clojure.set :as set]
            [systemic.core :as sys :refer [defsys]]
            [manifold.stream :as s]
            [dates.tick :as dates.tick]))

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
    (mapcat org-crud/nested-item->flattened-items)
    (map (fn [item]
           (cond
             (:org/closed item)
             (assoc item
                    ;; TODO this immediately falls out of date, heh
                    ;; need to calc and include a timer on the FE
                    :org/closed-since
                    (-> item :org/closed
                        dates.tick/parse-time-string
                        dates.tick/human-time-since))

             :else item)))))

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
