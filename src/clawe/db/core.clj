(ns clawe.db.core
  "Exposes functions for working with a datalevin database via `dtlv`."
  (:require [babashka.process :as process :refer [$]]
            [clojure.string :as string]))

;; TODO defsys and configuration
(def clawe-db-filepath "/home/russ/russmatney/clawe/clawedb")

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
        "(def conn (get-conn \"" clawe-db-filepath "\"))"
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
;; Transact!
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact!
  "Executes the passed transactions against the clawe-db via `dtlv`."
  [txs]
  (->
    (wrap-exec (str "(transact! conn " txs ")"))))

(comment
  (transact! [{:name "Datalevin"}])

  (transact! [{:some-workspace "Clawe"}
              {:some-other-data "jaja"}
              ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query
  "Execs a query via `dtlv`, returning the result."
  [q & args]
  (wrap-exec (str "(q (quote " q ") @conn "
                  (when (seq args) (->> args
                                        (apply str)))
                   ")")))

(comment
  (query '[:find ?e :where [?e :name ?n]])
  (query '[:find (pull ?e [*]) :where [?e :name ?n]])
  (query '[:find (pull ?e [*]) :in $ ?inp :where [?e :some-other-data ?inp]]
         ;; quoted string... gross!
         "\"jaja\"")
  )
