(ns api.focus
  (:require [garden.core :as garden]
            [org-crud.core :as org-crud]
            [org-crud.update :as org-crud.update]
            [systemic.core :as sys :refer [defsys]]
            [manifold.stream :as s]
            [dates.tick :as dates.tick]
            [clojure.string :as string]
            [babashka.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; domain
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (->>
    (garden/last-modified-paths)
    (take 3)))

(defn ->repo-root [repo-id]
  (str (fs/home) "/" repo-id))

(defn repo-todo-files []
  (let [repo-roots ["russmatney/dino"
                    "russmatney/org-crud"
                    "russmatney/clawe"
                    "teknql/fabb"]]
    (->> repo-roots
         (map ->repo-root)
         (mapcat (fn [root-path]
                   [(str root-path "/todo.org")
                    (str root-path "/readme.org")
                    ;; one day, source todos from code files
                    ]))
         (filter fs/exists?))))

(defn relevant-org-items []
  (->>
    (concat
      (garden/daily-paths 7)
      (garden/basic-todo-paths)
      (->> (garden/last-modified-paths) (take 30))
      (repo-todo-files))
    (distinct)
    (map org-crud/path->nested-item)))

(defn prep-todo [item]
  (cond
    (:org/closed item)
    (assoc item
           ;; TODO this immediately falls out of date, heh
           ;; need to calc and include a timer on the FE
           :org/closed-since
           (-> item :org/closed
               dates.tick/parse-time-string
               dates.tick/human-time-since))

    :else item))

;; (def opt-in-tags #{"goal" "goals" "focus"})

(defn todays-goals
  "Top level items tagged 'focus' or 'goals'"
  []
  (->>
    (relevant-org-items)
    ;; (mapcat :org/items)
    ;; include everything?
    ;; (filter (fn [it]
    ;;           (or
    ;;             ;; include any todos
    ;;             (:org/status it)
    ;;             ;; or any child of an opt-in tag
    ;;             ((comp seq #(set/intersection opt-in-tags %) :org/tags) it))))
    (mapcat org-crud/nested-item->flattened-items)
    (filter :org/status)
    (map prep-todo)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tag crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-tag [item tag]
  (org-crud.update/update! item {:org/tags tag}))

(defn remove-tag [item tag]
  (org-crud.update/update! item {:org/tags [:remove tag]}))

(comment
  (->>
    (todays-goals)
    (filter (comp #(string/includes? % "pluggs movement demo") :org/name))
    first
    (#(add-tag % "pluggs"))))
