(ns api.todos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [garden.db :as garden.db]
   [db.core :as db]
   [wing.core :as w]
   [garden.core :as garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB todo crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-todos-db []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where [?e :doctor/type :type/todo]])
    (map first)))

(comment
  (count (list-todos-db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn relevant-org-todos []
  (->> (concat
         (garden/daily-paths 14)
         (->> (garden/last-modified-paths) (take 30)))
       (distinct)
       garden/paths->flattened-garden-notes
       (filter :org/status)
       (map garden.db/merge-db-item)))

(comment
  (count (relevant-org-todos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-todos []
  (let [org-todos (relevant-org-todos)
        db-todos  (list-todos-db)
        ;; TODO merge instead of concat/dedupe
        all       (->> (concat org-todos db-todos)
                       (w/distinct-by :org/name-string))]
    all))

(defsys ^:dynamic *todos-stream*
  :start (s/stream)
  :stop (s/close! *todos-stream*))

(defn push-todos
  ([] (push-todos nil))
  ([todos] (s/put! *todos-stream* (or todos (build-todos)))))
