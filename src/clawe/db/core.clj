(ns clawe.db.core
  "Exposes functions for working with a datalevin database via `dtlv`."
  (:require [babashka.process :as process :refer [$]]
            [clojure.string :as string]))

;; TODO defsys and configuration
(def clawe-db-filepath "/home/russ/russmatney/clawe/clawedb")

(def schema {:scratchpad.db/id
             {:db/valueType :db.type/string
              :db/unique :db.unique/identity}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Execute via dtlv
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exec
  "Executes a passed command via dtlv."
  [cmd]
  (try
    (->
      ^{:out :string}
      ($ dtlv exec ~cmd)
      process/check
      :out)
    (catch Exception dtlv-e
      (let [d (ex-data dtlv-e)]
        (println "dtlv exception")
        (println "command" (:cmd d))
        (println "out" (:out d))
        (-> d
            :out
            string/trim
            string/split-lines
            last)))))

(comment
  (exec "some command"))

(defn wrap-exec [cmd]
  (->
    (exec
      (str
        "(def conn (get-conn \"" clawe-db-filepath "\" " schema "))"
        "\n"
        cmd
        "\n"
        "(close conn)"))
    string/trim
    string/split-lines
    second
    read-string))

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

(defn transact
  "Executes the passed transactions against the clawe-db via `dtlv`."
  [txs]
  (->
    (wrap-exec (str "(transact! conn " txs ")"))))

(comment
  (transact [{:name "Datalevin"}])
  (transact [{:last "value"
               :multi 5
               :key ["val" #{"hi"}]}])

  (transact [{:some-workspace "Clawe"}
              {:some-other-data "jaja"}
              ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->query-arg [arg]
  (cond :else (str arg)))

(defn args->query-args [args]
  (->> args (map ->query-arg) (apply str)))

(defn query
  "Execs a query via `dtlv`, returning the result."
  [q & args]
  (wrap-exec (str "(q (quote " q ") @conn "
                  (when (seq args) (args->query-args args))
                   ")")))

(comment
  ;; pull just ent ids
  (query '[:find ?e :where [?e :name ?n]])

  ;; pull whole maps of vals
  (query '[:find (pull ?e [*]) :where [?e :name ?n]])

  ;; fetch with arg
  (query '[:find (pull ?e [*]) :in $ ?inp :where [?e :some-other-data ?inp]]
         ;; quoted string... gross!
         "\"jaja\"")

  ;; fetch by :db/id
  (query '[:find (pull ?e [*]) :in $ ?e :where [?e _ _]]
         16)

  ;; or, and
  (query '[:find (pull ?e [*])
           :where (or
                    [?e :name "Datalevin"]
                    [?e :last "value"])
           ]))
