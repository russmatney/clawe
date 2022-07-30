(ns expo.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as ds]
   [expo.config :as config]
   [clojure.edn :as edn]
   [taoensso.timbre :as log]))

;; NOTE this is the datascript db that is serialized for the blog, not the internal datalevin db

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj datascript api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-db-from-file []
  (->>
    (slurp (config/expo-db-path))
    (edn/read-string {:readers ds/data-readers})))

(declare write-db-to-file)

;; TODO handle no-db case (i.e. no file?)
;; TODO handle schema updates
(defsys *conn*
  :start (let [conn (-> (read-db-from-file) (ds/conn-from-db))]
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
