(ns defthing.db
  "Exposes functions for working with a datalevin database."
  (:require
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [defthing.defcom :refer [defcom]]
   [defthing.config :as defthing.config]
   [wing.core :as w]
   [systemic.core :refer [defsys] :as sys]

   [pod.huahaiy.datalevin :as d]
   [taoensso.timbre :as log]))


(def db-schema
  {:topbar/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}
   :test/id
   {:db/valueType :db.type/uuid
    :db/unique    :db.unique/identity}
   :misc/id
   {:db/valueType :db.type/uuid
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

   :org/tags
   {:db/cardinality :db.cardinality/many}})


;; TODO close connections on server shutdown?
;; TODO clean up dead connections at startup?
(defsys *db-conn*
  :start (do
           (log/info "Starting new datalevin db conn")
           (d/get-conn (defthing.config/db-path) db-schema))
  :stop (d/close *db-conn*))


(comment
  *db-conn*
  (sys/start! `*db-conn*))


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
;; transact helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def converted-type-map
  ;; TODO bb compatability
  ;; {ZonedDateTime t/inst}
  {}
  )

(defn convert-matching-types [map-tx]
  (->> map-tx
       (map (fn [[k v]]
              (if-let [convert-fn (get converted-type-map (type v))]
                [k (convert-fn v)]
                [k v])))
       (into {})))

(def supported-types
  (->> [6 1.0 "hi" :some-keyword true
        (java.lang.Integer. 3)
        #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"
        ;; (t/inst)
        #{"some" "set" :of/things 5}]
       (map type)
       (into #{})))

(defn supported-type-keys [m]
  (->>
    m
    (filter (fn [[k v]]
              (let [t (type v)]
                (if (supported-types t)
                  true
                  (do
                    (when-not (nil? v)
                      (log/debug "unsupported type" t v k))
                    nil)))))
    (map first)))

(comment
  (supported-type-keys {:hello         "goodbye"
                        :some-int      5
                        :some-neg-int  -7
                        :some-java-int (java.lang.Integer. 3)
                        :some-float    1.0
                        :some-bool     false
                        :some-keyword  :keyword
                        :some-uuid     #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"
                        :some-fn       (fn [] (print "complexity!"))
                        :some-set      #{"hi" "there"}}))

(defn drop-unsupported-vals
  "Drops unsupported map vals. Drops nil. Only `supported` types
  get through."
  [map-tx]
  (let [supported-keys   (supported-type-keys map-tx)
        [to-tx rejected] (w/partition-keys map-tx supported-keys)]
    (when (seq rejected)
      (log/debug "defthing db transact rejected vals" rejected))
    (->> to-tx
         ;; might be unnecessary b/c it's not 'supported'
         (remove (comp nil? second))
         (into {}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact [txs]
  ;; no-ops if already started
  (sys/start! `*db-conn*)
  (let [txs (if (map? txs) [txs]
                ;; force seq/vec before grabbing the connection
                ;; in case lazySeq needs to use the db
                (->> txs (into [])))
        txs (map (fn [tx] (if (map? tx)
                            (->> tx
                                 convert-matching-types
                                 drop-unsupported-vals)
                            tx))
                 txs)
        _   (println "txs count" (count txs))
        res (d/transact! *db-conn* txs)]
    res))

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
