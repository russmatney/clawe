(ns api.focus
  (:require
   [garden.core :as garden]
   [garden.db :as garden.db]
   [org-crud.core :as org-crud]
   [org-crud.update :as org-crud.update]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
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

(defn todays-goals
  "Top level items tagged 'focus' or 'goals'"
  []
  (->>
    (relevant-org-items)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tag crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-tag [item tag]
  (org-crud.update/update! item {:org/tags tag}))

(defn remove-tag [item tag]
  (org-crud.update/update! item {:org/tags [:remove tag]}))

(defn remove-priority [item]
  (org-crud.update/update! item {:org/priority nil}))

(comment
  (->>
    (todays-goals)
    (filter (comp #(string/includes? % "pluggs movement demo") :org/name))
    first
    (#(add-tag % "pluggs"))))
