(ns api.todos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [babashka.fs :as fs]
   [defthing.db :as db]
   [wing.core :as w]
   [dates.tick :as dt]
   [garden.core :as garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB todo crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn upsert-todo-db [item]
  (let [existing (get-todo-db item)
        merged   (merge existing item)
        merged   (update merged :org/id #(or % (java.util.UUID/randomUUID)))
        merged   (assoc merged :doctor/type :type/todo)]
    (db/transact [merged])))

(defn list-todos-db []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where [?e :doctor/type :type/todo]])
    (map first)))

(comment
  (db/query
    '[:find (pull ?e [*])
      :where
      [?e :todo/status :status/in-progress]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def org-item-date-keys
  #{:org/closed
    :org/scheduled
    :org/deadline
    :org.prop/archive-time
    :org.prop/created-at})

(defn item->parsed-date-fields [item]
  (->> org-item-date-keys
       (map (fn [k]
              [k (-> item k dt/parse-time-string)]))
       (into {})))

(defn garden-note->todo
  [{:org/keys [name source-file status] :as item}]
  (let [parsed-date-fields (item->parsed-date-fields item)]
    (->
      item
      (assoc :todo/name name
             :todo/file-name (str (-> source-file fs/parent fs/file-name) "/" (fs/file-name source-file))
             :todo/status status)
      (merge parsed-date-fields))))

(defn build-org-todos []
  (->>
    (garden/paths->flat-garden-notes
      (concat
        (garden/daily-paths 14)
        (garden/workspace-paths)
        (garden/basic-todo-paths)))
    (filter :org/status) ;; this is set for org items with a todo state
    (map garden-note->todo)
    (map #(merge % (get-todo-db %)))))

(comment
  (->> (build-org-todos)
       (filter :todo/status)
       (group-by :todo/status)
       (map (fn [[s xs]]
              [s (count xs)])))

  (->>
    (build-org-todos)
    (take 7))

  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :todo/status :status/in-progress]])))

(defn sorted-todos []
  (->> (build-org-todos)
       (sort-by :db/id)
       reverse
       (sort-by :todo/status)
       (sort-by (comp not #{:status/in-progress} :todo/status))))

(comment
  (sorted-todos))

(defn recent-org-items []
  ;; TODO consider db pulling/overwriting/syncing
  (->>
    (garden/paths->flat-garden-notes
      (concat
        (garden/daily-paths 14)
        (garden/monthly-archive-paths 3)
        (garden/workspace-paths)
        (garden/basic-todo-paths)))
    (map garden-note->todo)))

(comment
  (recent-org-items))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-todos []
  (let [org-todos (build-org-todos)
        db-todos  (list-todos-db)
        ;; should these be merged instead of concat/deduped?
        all       (->> (concat org-todos db-todos)
                       (w/distinct-by :org/name))]
    all))

(comment
  (->>
    (get-todos)
    (filter :db/id)
    (count)))

(defsys *todos-stream*
  :start (s/stream)
  :stop (s/close! *todos-stream*))

(comment
  (sys/start! `*todos-stream*))

(defn update-todos []
  (s/put! *todos-stream* (get-todos)))
