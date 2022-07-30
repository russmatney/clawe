(ns defthing.db
  "Exposes functions for working with a datalevin database."
  (:require
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [defthing.defcom :refer [defcom]]
   [defthing.config :as defthing.config]
   [defthing.db-helpers :as db-helpers]
   [systemic.core :refer [defsys] :as sys]
   [taoensso.timbre :as log]

   #?@(:bb [[pod.huahaiy.datalevin :as d]]
       :clj [[datalevin.core :as d]])))


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
   {:db/cardinality :db.cardinality/many}

   :org/link-ids
   {:db/cardinality :db.cardinality/many}
   :org/parent-ids
   {:db/cardinality :db.cardinality/many}

   :org/parent-names
   {:db/cardinality :db.cardinality/many}

   :org/tags
   {:db/cardinality :db.cardinality/many}
   :org/urls
   {:db/cardinality :db.cardinality/many}})


;; TODO close connections on server shutdown?
;; TODO clean up dead connections at startup?
(defsys *db-conn*
  :start
  (let [path (defthing.config/db-path)]
    (log/info "Starting datalevin db conn" path)
    (d/get-conn path db-schema))
  :stop (d/close *db-conn*))

(comment
  *db-conn*
  (sys/start! `*db-conn*)

  (d/clear *db-conn*)

  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dump
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dump
  "Return the database.

  TODO could be parsed...
  TODO this seemes to flatline ... maybe needs to stream?
  "
  []
  (->
    ($ dtlv -d ~(defthing.config/db-path) -g dump)
    process/check
    :out
    slurp
    string/split-lines))

(comment
  (->>
    (dump)
    (take-last 5)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact
  ([txs] (transact txs nil))
  ([txs opts]
   (sys/start! `*db-conn*)
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
       (let [res (d/transact! *db-conn* txs)]
         ;; (when (> (:datoms-transacted res) 0)
         ;;   (log/info "txs" txs))
         res)
       (catch Exception e
         (log/warn "Exception while transacting data!" e)
         (when on-error (on-error txs))
         ;; TODO consider default retry with fewer txs
         (when on-retry (on-retry txs)))))))

(comment
  (transact [{:name "Datalevin"}])
  (transact [{:last "value" :multi 5 :key ["val" #{"hi"}]}])
  (transact [{:some-workspace "Clawe"} {:some-other-data "jaja"}])
  (transact [{:some-workspace "Clawe"
              :some-nil-val   nil}
             {:some-other-data "jaja"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [q & args]
  (sys/start! `*db-conn*)
  (let [res (if args
              (apply d/q q (d/db *db-conn*) args)
              (d/q q (d/db *db-conn*)))]
    res))

(comment
  *db-conn*
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
                    [?e :name "Datalevin"]
                    [?e :last "value"])]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Retract
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn retract [ent attr value]
;;   (sys/start! `*db-conn*)
;;   (let [res
;;         (if value
;;           (apply d/retract ent (d/db *db-conn*) args)
;;           (d/retract q (d/db *db-conn*)))]
;;     res))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom dump-db
  "Dump the datalevin db"
  (do
    (println "defthing.db/dump-db called")
    (doall
      (->>
        (dump)
        (map println)))))

(defcom query-db
  "Query the datalevin db"
  (fn [_ args]
    (println "defthing.db/query-db called" args)
    (doall
      (->>
        (query '[:find (pull ?e [*])
                 :where [?e :doctor/type :type/garden]])
        (take 3)
        (map println)))))
