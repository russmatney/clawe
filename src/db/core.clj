(ns db.core
  "Exposes functions for working with a datascript database."
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [datascript.core :as d]
   [dates.tick :as dates.tick]
   [db.schema :refer [schema]]
   [db.config :as db.config]
   [db.helpers :as helpers]
   [systemic.core :refer [defsys] :as sys]
   [taoensso.timbre :as log]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj datascript api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-from-file []
  (let [db-file (db.config/db-path)]
    (if (fs/exists? db-file)
      (->
        (edn/read-string {:readers d/data-readers} (slurp db-file))
        ;; support swapping in the schema to allow for schema updates
        (d/datoms :eavt)
        (d/conn-from-datoms schema)
        d/db)
      (do
        (log/info "No db file found creating empty one")
        (d/empty-db schema)))))

(comment
  (slurp (db.config/db-path))
  (db-from-file))

(declare write-db-to-file)

(defsys ^:dynamic *conn*
  :start
  (let [conn (d/conn-from-db (db-from-file))]
    (d/listen! conn :db-backup-writer
               ;; TODO debounce to every few seconds, or during idle time
               (fn [_] (write-db-to-file)))
    conn)
  :stop
  (write-db-to-file)
  (d/unlisten! *conn* :db-backup-writer))


(defn print-db []
  (sys/start! `*conn*)
  (pr-str (d/db *conn*)))

(defn write-db-to-file []
  #_(log/debug "Writing Doctor DB to file")
  (spit (db.config/db-path) (print-db)))

(defn clear-db []
  (fs/delete-if-exists (fs/file (db.config/db-path))))

(defn reload-conn []
  (if (and `*conn* (sys/running? `*conn*))
    (sys/restart! `*conn*)
    (sys/start! `*conn*)))

(comment
  (reload-conn)
  (clear-db)
  ;; if not running, this restart restarts _everything_
  (sys/start! `*conn*)
  (sys/restart! `*conn*)

  (:schema @*conn*)

  (write-db-to-file)

  (count (d/datoms @*conn* :eavt))
  ;; 379672
  ;; 368801

  (count (d/datoms @*conn* :aevt :doctor/type))
  ;; 16983
  ;; 17606
  (frequencies (->> (d/datoms @*conn* :aevt :doctor/type) (map :v)))
  #:type{:lichess-game 623, :screenshot 523, :repo 88, :commit 483, :note 12728, :todo 3038, :wallpaper 117, :pomodoro 6}
  #:type{:screenshot 523, :repo 88, :commit 483, :note 12728, :todo 3038, :wallpaper 117, :pomodoro 6}


  ;; attr frequency
  (->>
    (frequencies (->> (d/datoms @*conn* :aevt) (map :a)))
    (filter (fn [[_val ct]]
              (> ct 1)))
    (sort-by second >)
    (take 20))

  ;; value frequency
  (->>
    (frequencies (->> (d/datoms @*conn* :aevt) (map :v)))
    (filter (fn [[_val ct]]
              (> ct 1)))
    (sort-by second >)
    (take 20))

  ;; all attrs by :doctor/type
  (->> (d/datoms @*conn* :aevt :doctor/type)
       (map :v)
       (distinct)
       (map (fn [type]
              [type
               (->>
                 (d/q '[:find ?a
                        :in $ ?type
                        :where
                        [?e :doctor/type ?type]
                        [?e ?a _]]
                      @*conn* type)
                 (map first)
                 (into #{}))]))
       (into {}))

  ;; entity counts-of-things
  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e _ _]
           (not [?e :doctor/type _])]
         @*conn*)
    (map first)
    count)


  ;; retracting entities
  (->>
    (d/datoms @*conn* :aevt :doctor/type)
    (filter (comp #{:type/lichess-game} :v))
    (map :e)
    (map (fn [e]
           [:db.fn/retractEntity e]))
    (d/transact *conn*)
    )

  ;; retracting attributes
  (->>
    (d/datoms @*conn* :aevt :org/body-string)
    (map :e)
    (map (fn [e]
           [:db/retract e :org/body-string]))
    (d/transact *conn*)
    )

  ;; reingest

  (declare transact)
  (->>
    (d/datoms @*conn* :eavt)
    (partition-all 20000)
    (map
      #(d/transact *conn* %)))

  ;; all entities
  (d/pull-many @*conn* '[*] (d/q '[:find ?e :where [?e :db/id _]] @*conn*))
  (d/q '[:find (pull ?e [*]) :where [?e :db/id _]] @*conn*)

  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e :doctor/type _]
           #_ [(missing? ?e :event/timestamp)]]
         @*conn*)
    (map first)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact
  ([txs] (transact txs nil))
  ([txs opts]
   (sys/start! `*conn*)
   (let [on-error (:on-error opts)
         on-retry (:on-retry opts)
         verbose? (:verbose? opts)
         txs      (->>
                    (if (map? txs) [txs] txs)
                    (map (fn [tx] (if (map? tx)
                                    (->> tx
                                         helpers/convert-matching-types
                                         (helpers/drop-unsupported-vals opts))
                                    tx)))
                    (into []))]
     (when verbose?
       (log/debug "Transacting records" (count txs)))
     (try
       (d/transact! *conn* txs)
       (catch Exception e
         (log/warn "Exception while transacting data!" e)
         (when on-error (on-error txs))
         ;; TODO consider default retry with fewer txs
         (when on-retry (on-retry txs)))))))

(comment
  (transact [{:name "Datascript"}])
  (transact [{:workspace/title "datascript" :my/misc "valvalval"}])
  (transact [{:now       (dates.tick/now)
              :something "useful"}])
  (declare query)
  (query '[:find (pull ?e [*]) :where [?e :workspace/title ?n]])
  (transact [{:last "value" :multi 5 :key ["val" #{"hi"}]}])
  (transact [{:some-workspace "Clawe"} {:some-other-data "jaja"}])
  (transact [{:some-workspace "Clawe"
              :some-nil-val   nil}
             {:some-other-data "jaja"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn datoms [& args]
  (sys/start! `*conn*)
  (apply d/datoms (d/db *conn*) args))

(defn query [q & args]
  (sys/start! `*conn*)
  (if args
    (apply d/q q (d/db *conn*) args)
    (d/q q (d/db *conn*))))

(comment
  (->
    (d/datoms (d/db *conn*) :eavt)
    reverse)

  *conn*
  (->>
    (query '[:find (pull ?e [*]) :where [?e _ _]])
    (map first)
    (sort-by :db/id))

  ;; pull just ent ids
  (query '[:find ?e :where [?e :name ?n]])

  ;; pull whole maps of vals
  (query '[:find [(pull ?e [*])] :where [?e :name ?n]])
  ;; notice the difference when you wrap the :find as a vector
  (query '[:find (pull ?e [*]) :where [?e :name ?n]])

  ;; fetch with arg
  (query '[:find [(pull ?e [*])] :in $ ?inp :where [?e :some-other-data ?inp]]
         "jaja")

  ;; fetch by :db/id
  (query '[:find [(pull ?e [*])] :in $ ?e :where [?e _ _]]
         1)

  ;; or, and
  (query '[:find [(pull ?e [*])]
           :where (or
                    [?e :name "Datascript"]
                    [?e :last "value"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Retract
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn retract! [txs]
  (when (seq txs)
    (sys/start! `*conn*)
    (let [txs (if (coll? (first txs)) txs [txs])]
      ;; hmm just a transact wrapper i guess
      (d/transact *conn* txs))))

(defn retract-entities [ent-ids]
  (let [ent-ids (if (coll? ent-ids) ent-ids [ent-ids])]
    (->> ent-ids (map (fn [ent-id] [:db.fn/retractEntity ent-id]))
         retract!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-db
  "Query the datascript db"
  ;; no args supported yet - but this is here to be tested from the cli
  ([] (query-db nil))
  ([args]
   (log/info "db.core/query-db called" args)
   (doall
     (->>
       (query '[:find (pull ?e [*])
                :where
                [?e :doctor/type ?type]
                [(contains? #{:type/note :type/todo
                              ;; include other types?
                              } ?type)]
                ])
       (take 3)))))

(comment
  (query-db))
