(ns defthing.db
  "Exposes functions for working with a datalevin database via `dtlv`."
  (:require
   [babashka.pods :as pods]
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]))

(pods/load-pod "dtlv")
(require '[pod.huahaiy.datalevin :as d])

;; TODO defsys and configuration
(def defthing-db-filepath (zsh/expand "~/russmatney/clawe/defthingdb"))


(def db-schema nil)

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
  (->> [6 "hi" :some-keyword true (random-uuid)]
       (map type)
       (into #{})))

(defn supported-type-keys [m]
  (->>
    m
    (filter (fn [[_k v]] (supported-types (type v))))
    (map first)))

(comment
  (supported-type-keys {:hello        "goodbye"
                        :some-int     5
                        :some-bool    false
                        :some-keyword :keyword
                        :some-uuid    (random-uuid)
                        :some-fn      (fn [] (print "complexity!"))}))

(defn drop-unsupported-vals
  "Drops unsupported map vals. Drops nil. Only `supported` types
  get through."
  [map-tx]
  (let [supported-keys (supported-type-keys map-tx)]
    (->> (select-keys map-tx supported-keys)
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
