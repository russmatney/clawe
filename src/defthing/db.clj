(ns defthing.db
  "Exposes functions for working with a datalevin database via `dtlv`."
  (:require
   [babashka.pods :as pods]
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]
   [wing.core :as w]))

(pods/load-pod "dtlv")
(require '[pod.huahaiy.datalevin :as d])

;; TODO defsys and configuration
(def defthing-db-filepath (zsh/expand "~/russmatney/clawe/newdb"))

(def db-schema
  {:topbar/id
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
    :db/unique    :db.unique/identity}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dump
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dump
  "Return the database.

  TODO could be parsed...
  "
  []
  (->
    ($ dtlv -d ~defthing-db-filepath -g dump)
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

(def supported-types
  (->> [6 1.0 "hi" :some-keyword true
        (java.lang.Integer. 3)
        #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"]
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
                    (println "unsupported type" t v k)
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
                        :some-fn       (fn [] (print "complexity!"))}))

(defn drop-unsupported-vals
  "Drops unsupported map vals. Drops nil. Only `supported` types
  get through."
  [map-tx]
  (let [supported-keys   (supported-type-keys map-tx)
        [to-tx rejected] (w/partition-keys map-tx supported-keys)]
    (when (seq rejected)
      (println "defthing db transact rejected vals" rejected))
    (->> to-tx
         ;; might be unnecessary b/c it's not 'supported'
         (remove (comp nil? second))
         (into {}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact [txs]
  ;; TODO refactor into defsys conn ?
  (let [txs  (if (map? txs) [txs]
                 ;; force seq/vec before grabbing the connection
                 ;; in case lazySeq needs to use the db
                 (->> txs (into [])))
        conn (d/get-conn defthing-db-filepath db-schema)
        txs  (map (fn [tx] (if (map? tx)
                             (drop-unsupported-vals tx)
                             tx))
                  txs)
        _    (println "txs" txs)
        res  (d/transact! conn txs)]
    (d/close conn)
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
  (let [conn (d/get-conn defthing-db-filepath db-schema)
        res
        (if args
          (apply d/q q (d/db conn) args)
          (d/q q (d/db conn)))]
    (d/close conn)
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
;;   (let [conn (d/get-conn defthing-db-filepath db-schema)
;;         res
;;         (if value
;;           (apply d/retract ent (d/db conn) args)
;;           (d/retract q (d/db conn)))]
;;     (d/close conn)
;;     res)
;;   )
