(ns clawe.db.core
  "Exposes functions for working with a datalevin database via `dtlv`."
  (:require
   [babashka.pods :as pods]
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]))

(pods/load-pod "dtlv")
(require '[pod.huahaiy.datalevin :as d])

;; TODO defsys and configuration
(def clawe-db-filepath "/home/russ/russmatney/clawe/clawedb")

(def schema {:scratchpad.db/id
             {:db/valueType :db.type/string
              :db/unique :db.unique/identity}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB Dump
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dump
  "Return the database.

  TODO could be parsed...
  "
  []
  (->
    ($ dtlv -d ~clawe-db-filepath -g dump)
    process/check
    :out
    slurp
    string/split-lines))

(comment
  (dump))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact [txs]
  ;; TODO refactor into defsys conn ?
  (let [conn (d/get-conn clawe-db-filepath schema)
        res (d/transact! conn txs)]
    (d/close conn)
    res))

(comment
  (transact [{:name "Datalevin"}])
  (transact [{:last "value" :multi 5 :key ["val" #{"hi"}]}])
  (transact [{:some-workspace "Clawe"} {:some-other-data "jaja"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [q & args]
  (let [conn (d/get-conn clawe-db-filepath schema)
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
  (query '[:find (pull ?e [*]) :where [?e :name ?n]])

  ;; fetch with arg
  (query '[:find (pull ?e [*]) :in $ ?inp :where [?e :some-other-data ?inp]]
         "jaja")

  ;; fetch by :db/id
  (query '[:find (pull ?e [*]) :in $ ?e :where [?e _ _]]
         16)

  ;; or, and
  (query '[:find (pull ?e [*])
           :where (or
                    [?e :name "Datalevin"]
                    [?e :last "value"])
           ])
  )
