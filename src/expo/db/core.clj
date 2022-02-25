(ns expo.db.core
  "core database ns."
  (:require
   [datahike.api :as d]
   [datahike.core :as d.core]
   [datahike.migrate :as d.mig]
   [systemic.core :as sys :refer [defsys]]
   [tick.core :as t]
   [expo.config :as config]
   [expo.db.schema :as db.schema]
   [expo.db.serial :as db.serial]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *db-file* (:db-file config/*config*))
(defsys *db-migration-base-file* (:db-temp-migration-file config/*config*))

(defn supported-keys []
  (->> (db.schema/schema)
       (map :db/ident)
       (into (set []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config and connection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *cfg*
  :deps [*db-file*]
  :start
  (atom
    {:store              {:backend :file
                          :path    *db-file*}
     :keep-history?      true
     :schema-flexibility :write
     :name               "yodo-db"
     :initial-tx         (db.schema/schema)}))

(defsys *conn*
  "Starting this system creates the database if it does not exist.
  Returns a usable database connection as an atom."
  :deps
  [*db-file*
   *db-migration-base-file*]
  :closure
  (when-not (d/database-exists? @*cfg*)
    (println "Creating new datahike database!")
    (d/create-database @*cfg*))
  (let [c (d/connect @*cfg*)]
    {:value c
     :stop  (fn [] (d/release c))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; database lifecycle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recreate-db!
  "Completely deletes and recreates the database."
  []
  (println "Deleting existing datahike database")
  (d/delete-database @*cfg*)
  (sys/restart! `*conn*))

(defn migration-file []
  (str *db-migration-base-file* "-" (inst-ms (t/now))))

(defn migrate-db!
  "Copies the db out to flat file, recreates the connection,
  then re-imports the data.
  Should work fine, right?

  TODO: does this wipe the db if the new schema fails to load?
  "
  []
  (let [mig-file (migration-file)]
    (println "Migrating existing datahike database via:" mig-file)
    ;; export to backup file
    (d.mig/export-db @*conn* mig-file)
    ;; restart cfg to load the new schema
    (sys/restart! `*cfg*)
    ;; delete the database
    (d/delete-database @*cfg*)
    ;; restart conn to recreate the db
    (sys/restart! `*conn*)
    ;; import from backup file
    (d.mig/import-db *conn* mig-file)))

(comment
  (sys/start! `*conn*)
  (sys/restart! `*conn*)

  (d/history @*conn*)
  (recreate-db!)
  (migrate-db!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public datahike wrappers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact [txs]
  (let [txs (if (map? txs) [txs] txs)]
    (println (str "transacting " (count txs) " items."))
    (->> txs
         (map db.serial/encode-tx)
         (into [])
         (d/transact *conn*))))

(defn transact! [txs]
  (let [txs (if (map? txs) [txs] txs)]
    (->> txs
         (map db.serial/encode-tx)
         (into [])
         (d/transact! *conn*))))

;; TODO decode query results for all nested maps?
;; TODO handle extra nesting in `q`
(defn q [query & args] (apply d/q query @*conn* args))

(defn datoms [& [fst & rest]]
  (if (map? fst)
    (if (:history fst)
      (apply d/datoms (d/history @*conn*) rest)
      (apply d/datoms @*conn* rest))
    (apply d/datoms @*conn* (cons fst rest))))

;; TODO decoding ent fields can be tricky b/c fields are lazy...
;; probably should find the datahike way of deserializing
(defn entity [& args] (apply d/entity @*conn* args))

(defn pull [& args]
  (-> (apply d/pull @*conn* args)
      db.serial/decode-entity))

(defn pull-many [& args]
  (->> (apply d/pull-many @*conn* args)
       ;; not sure why a nested list is returned
       ((fn [ents] (if (coll? ents) ents [ents])))
       (map db.serial/decode-entity)))

(defn listen! [& args] (apply d.core/listen! *conn* args))
(defn unlisten! [& args] (apply d.core/unlisten! *conn* args))

(comment
  (q '[:find (pull ?entity [*])
       :where
       [?entity :todo/title ?title]])

  (q '[:find (pull ?entity [*])
       :in $ ?entity
       :where
       [?entity :db/id _]]
     21)

  (datoms :eavt)

  (d/seek-datoms @*conn* :eavt 15)
  (d/datoms @*conn* :avet :item/name)

  ;; migrate a field
  (->> (db.schema/date-attrs)
       (map (fn [attr]
              (println attr)
              (datoms :avet attr))))

  (datoms :avet :item/created-at)

  ;; data migration
  (->> (datoms :eavt)
       (filter (comp #{:todo/archived-at} :a))
       (map (fn [dat] (assoc dat :a :item/archived-at)))
       (transact)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn last-tx-id []
  (->
    (q '[:find (max 1 ?tx)
         :where [?tx :db/txInstant]])
    ;; yep.
    first first first))

(defn last-tx-entities
  "Returns the entities involved in the last transaction."
  []
  (let [tx (last-tx-id)]
    (when tx
      (->> (q '[:find (pull ?e [*])
                :in $ ?tx
                :where
                [?e _ _ ?tx]]
              tx)
           (map first)
           ;; remove transaction entity
           (remove :db/txInstant)
           ;; convert date types...
           (map db.serial/decode-entity)))))

(defn last-tx-entity
  "Note that txs can have multiple entities!
  Intended for testing convenience."
  [] (first (last-tx-entities)))

(comment
  (transact [{:item/name "hi"}
             {:item/name "bye"}
             {:item/name "duaa"}])

  (last-tx-entities)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entity-watcher
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn report->eids
  "Converts a transaction report to a list of entity ids.
  :datom-filter is a predicate for which datoms to source eids from."
  ([report] (report->eids report {:datom-filter (constantly true)}))
  ([{:keys [datom-filter]} report]
   (->> report
        :tx-data
        (group-by first)
        (filter (comp seq #(filter datom-filter %) second))
        (map first)
        (into (set [])))))

(defn entity-watcher
  "Listens and reacts to incoming db transactions via `listen!`.

  Filtered datoms (via `datom-filter`) are converted to a set of entity ids,
  which are by `fetch-entities` (defaults to `(pull-many '[*] entity-ids)`).

  The updated entities are passed to `on-update`.

  `:key` must be unique - repeated keys will disable a previous same-key watcher.

  Returns a systemic :closure map.

  Example for calling `push-todos` with every entity that sees a :todo/title tx.
  ```
  (defsys *todos-watcher*
       :deps [*todos-stream* db/*conn*]
       :closure (entity-watcher
                  {:key       :todos-watcher
                   :on-update push-todos
                   :datom-filter (comp #{:todo/title} :a)}))
  ```"
  [{:keys [key
           datom-filter
           fetch-entities
           on-update]}]
  (let [fetch-entities (or fetch-entities
                           (partial pull-many '[*]))
        db-update-handler
        (fn [report]
          ;; (def tx-report report)
          (when-let [ents (->> report
                               (report->eids {:datom-filter datom-filter})
                               fetch-entities
                               seq)]
            (on-update ents)))]
    {:value (listen! key db-update-handler)
     :stop  (fn [] (unlisten! key))}))
