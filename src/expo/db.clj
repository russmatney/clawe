(ns expo.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as d]
   [expo.config :as config]
   [clojure.edn :as edn]
   [taoensso.timbre :as log]
   [babashka.fs :as fs]))

(def db-schema
  {
   ;; unique string ids
   :commit/hash
   {:db/valueType :db.type/ref
    :db/unique    :db.unique/identity}
   :repo/directory
   {:db/valueType :db.type/ref
    :db/unique    :db.unique/identity}
   :lichess.game/id
   {:db/valueType :db.type/ref
    :db/unique    :db.unique/identity}
   :org/fallback-id
   {:db/valueType :db.type/ref
    :db/unique    :db.unique/identity}
   :org/id
   {:db/valueType :db.type/ref
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

(comment
  (d/empty-db db-schema))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj datascript api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-db-from-file []
  (let [db-file (config/expo-db-path)]
    (when (fs/exists? db-file)
      (->>
        (slurp db-file)
        (edn/read-string {:readers d/data-readers})))))

(declare write-db-to-file)

(defsys *conn*
  :start (let [raw-db (read-db-from-file)
               conn   (-> (or raw-db (d/empty-db db-schema)) (d/conn-from-db))]
           (d/listen! conn :db-writer (fn [_] (write-db-to-file)))
           conn)
  :stop
  (d/unlisten! *conn* :db-writer))

(defn print-db []
  (sys/start! `*conn*)
  (pr-str (d/db *conn*)))

(defn write-db-to-file []
  (log/info "Writing Expo DB to file")
  (spit (config/expo-db-path) (print-db)))

(defn clear-db []
  (fs/delete-if-exists (fs/file (config/expo-db-path))))

(comment
  (clear-db)
  (sys/restart! `*conn*)

  (write-db-to-file))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transact
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transact [txs]
  (sys/start! `*conn*)
  (d/transact! *conn* txs))

(comment
  (transact [{:some/new   :piece/of-data
              :with/attrs 23
              :and/other  :attrs/enum}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [q & args]
  (sys/start! `*conn*)
  (apply d/q q (d/db *conn*) args))

(comment
  (d/q '[:find (pull ?e [*])
         :where
         [?e :doctor/type :type/garden]]
       (d/db *conn*))

  (query '[:find (pull ?e [*])
           :where
           [?e :doctor/type :type/garden]])

  (query '[:find (pull ?e [*])
           :in $ ?inp
           :where
           [?e :some/new ?inp]]
         :piece/of-data))
