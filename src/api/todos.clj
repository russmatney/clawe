(ns api.todos
  (:require
   [taoensso.timbre :as log]
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [garden.db :as garden.db]
   [db.core :as db]
   [garden.core :as garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB todo crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-current-db-todos []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :doctor/type :type/todo]
        (or
          [?e :org/status :status/in-progress]
          ;; TODO remove tags from db when removed from org items
          #_[?e :org/tags "current"])])
    (map first)))

(comment
  (count (list-current-db-todos)))

(defn list-db-todos []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where [?e :doctor/type :type/todo]])
    (map first)))

(comment
  (count (list-db-todos)))

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

(defn ingest-relevant-org-todos []
  (db/transact (relevant-org-todos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-todos []
  (list-db-todos))

(defsys ^:dynamic *todos-stream*
  :start (s/stream)
  :stop (s/close! *todos-stream*))

(defsys ^:dynamic *current-todos-stream*
  :start (s/stream)
  :stop (s/close! *current-todos-stream*))

(defn push-todos []
  (log/info "Pushing todo data")
  (s/put! *current-todos-stream* (list-current-db-todos))
  (s/put! *todos-stream* (build-todos)))
