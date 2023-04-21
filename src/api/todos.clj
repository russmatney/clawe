(ns api.todos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [db.core :as db]
   [wing.core :as w]
   [garden.core :as garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB todo crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO use garden.db match merge helper
(defn get-todo-db
  "Matches on just :org/name for now."
  [item]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?name
        :where
        [?e :org/name ?name]
        [?e :doctor/type :type/todo]]
      (:org/name item))
    ffirst))

(defn list-todos-db []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :doctor/type :type/todo]])
    (map first)))

(comment
  (count
    (list-todos-db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO merge with api.focus logic
(defn build-org-todos []
  (->>
    (garden/paths->flattened-garden-notes
      (concat
        (garden/daily-paths 14)
        (->> (garden/last-modified-paths) (take 10))))
    (filter :org/status) ;; this is set for org items with a todo state
    (map #(merge % (get-todo-db %)))))

(comment
  (->>
    (build-org-todos)
    (take 7)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-todos []
  (let [org-todos (build-org-todos)
        db-todos  (list-todos-db)
        ;; TODO merge instead of concat/dedupe
        all       (->> (concat org-todos db-todos)
                       (w/distinct-by :org/name-string))]
    all))

(comment
  (->>
    (get-todos)
    (filter :db/id)
    (count)))

(defsys ^:dynamic *todos-stream*
  :start (s/stream)
  :stop (s/close! *todos-stream*))

(comment
  (sys/start! `*todos-stream*))

(defn update-todos []
  (s/put! *todos-stream* (get-todos)))
