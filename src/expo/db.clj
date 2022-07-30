(ns expo.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as ds]
   [expo.config :as config]
   [clojure.edn :as edn]
   [taoensso.timbre :as log]
   [babashka.fs :as fs]))

;; NOTE this is the datascript db that is serialized for the blog, not the internal datalevin db

(def db-schema
  {
   ;; unique string ids
   :git.commit/hash
   {:db/valueType :db.type/ref
    :db/unique    :db.unique/identity}
   :git.repo/directory
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
  (ds/empty-db db-schema))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj datascript api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-db-from-file []
  (let [db-file (config/expo-db-path)]
    (when (fs/exists? db-file)
      (->>
        (slurp db-file)
        (edn/read-string {:readers ds/data-readers})))))

(declare write-db-to-file)

(defsys *conn*
  :start (let [raw-db (read-db-from-file)
               conn   (-> (or raw-db (ds/empty-db db-schema)) (ds/conn-from-db))]
           (ds/listen! conn :db-writer (fn [_] (write-db-to-file)))
           conn)
  :stop
  (ds/unlisten! *conn* :db-writer))

(defn print-db []
  (sys/start! `*conn*)
  (pr-str (ds/db *conn*)))

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
  (ds/transact! *conn* txs))

(comment
  (transact [{:some/new   :piece/of-data
              :with/attrs 23
              :ands/other :attrs/enum}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; query
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [q & args]
  (sys/start! `*conn*)
  (apply ds/q q (ds/db *conn*) args))

(comment
  (ds/q '[:find (pull ?e [*])
          :where
          [?e :doctor/type :type/garden]]
        (ds/db *conn*))

  (query '[:find (pull ?e [*])
           :where
           [?e :doctor/type :type/garden]])

  (query '[:find (pull ?e [*])
           :in $ ?inp
           :where
           [?e :some/new ?inp]]
         :piece/of-data))
