(ns defthing.db
  "Exposes functions for working with a datascript database."
  (:require
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [defthing.defcom :refer [defcom]]
   [defthing.config :as defthing.config]
   [defthing.db-helpers :as db-helpers]
   [systemic.core :refer [defsys] :as sys]
   [taoensso.timbre :as log]
   [datascript.core :as d]
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [defthing.db :as db]
   [dates.tick :as dates.tick]))

(def db-schema
  {
   ;; uuids
   :topbar/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}
   :test/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}
   :misc/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}

   ;; unique string ids
   :scratchpad.db/id
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}
   :workspace/title
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}
   :git.commit/hash
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}
   :git.repo/directory
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}
   :lichess.game/id
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}

   :time/rn
   {:db/valueType :db.type/instant}

   ;; org attrs
   :org/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}
   :org/fallback-id
   {:db/valueType :db.type/string
    :db/unique    :db.unique/identity}

   ;; manys
   :org/link-text
   {:db/cardinality :db.cardinality/many}
   :org/linked-from
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   :org/links-to
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}
   :org/parents
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   ;; TODO how to do many, but still unique?
   :org/parent-names
   {:db/cardinality :db.cardinality/many}

   ;; TODO one day create unique tag entities with metadata
   :org/tags
   {:db/cardinality :db.cardinality/many}
   ;; TODO one day create unique url entities with metadata
   :org/urls
   {:db/cardinality :db.cardinality/many}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj datascript api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-from-file []
  (let [db-file (defthing.config/db-path)]
    (if (fs/exists? db-file)
      (->>
        (slurp db-file)
        (edn/read-string {:readers d/data-readers}))
      (do
        (log/info "No db file found creating empty one")
        (d/empty-db)))))

(comment
  (slurp (defthing.config/db-path))
  (db-from-file))

(declare write-db-to-file)

(defsys *conn*
  :start
  (let [raw-db (db-from-file)
        conn   (-> (or raw-db (d/empty-db db-schema)) (d/conn-from-db))]
    (d/listen! conn :db-backup-writer (fn [_] (write-db-to-file)))
    conn)
  :stop
  (d/unlisten! *conn* :db-backup-writer))

(defn print-db []
  (sys/start! `*conn*)
  (pr-str (d/db *conn*)))

(defn write-db-to-file []
  (log/info "Writing Expo DB to file")
  (spit (defthing.config/db-path) (print-db)))

(defn clear-db []
  (fs/delete-if-exists (fs/file (defthing.config/db-path))))

(comment
  (clear-db)
  (sys/restart! `*conn*)

  (write-db-to-file))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact
  ([txs] (transact txs nil))
  ([txs opts]
   (sys/start! `*conn*)
   (let [on-error (:on-error opts)
         on-retry (:on-retry opts)
         txs      (->>
                    (if (map? txs) [txs] txs)
                    (map (fn [tx] (if (map? tx)
                                    (->> tx
                                         db-helpers/convert-matching-types
                                         (db-helpers/drop-unsupported-vals opts))
                                    tx)))
                    (into []))]
     (log/debug "Transacting records" (count txs))
     (try
       (d/transact! *conn* txs)
       (catch Exception e
         (log/warn "Exception while transacting data!" e)
         (when on-error (on-error txs))
         ;; TODO consider default retry with fewer txs
         (when on-retry (on-retry txs)))))))

(comment
  (transact [{:name "Datascript"}])
  (transact [{:now       (dates.tick/now)
              :something "useful"}])
  (declare query)
  (query '[:find (pull ?e [*]) :where [?e :something ?n]])
  (transact [{:last "value" :multi 5 :key ["val" #{"hi"}]}])
  (transact [{:some-workspace "Clawe"} {:some-other-data "jaja"}])
  (transact [{:some-workspace "Clawe"
              :some-nil-val   nil}
             {:some-other-data "jaja"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [q & args]
  (sys/start! `*conn*)
  (let [res (if args
              (apply d/q q (d/db *conn*) args)
              (d/q q (d/db *conn*)))]
    res))

(comment
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

;; (defn retract [ent attr value]
;;   (sys/start! `*conn*)
;;   (let [res
;;         (if value
;;           (apply d/retract ent (d/db *conn*) args)
;;           (d/retract q (d/db *conn*)))]
;;     res))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-db
  "Query the datascript db"
  ([] (query-db nil))
  ([args]
   ;; no args supported yet, but this is here to be called from the cli
   (println "defthing.db/query-db called" args)
   (doall
     (->>
       (query '[:find (pull ?e [*])
                :where [?e :doctor/type :type/garden]])
       (take 3)
       (map println)))))

(comment
  (query-db))
