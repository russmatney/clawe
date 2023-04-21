(ns api.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [db.core :as db]
   [datascript.core :as d]
   [taoensso.timbre :as log]))

(defn last-modified-files->es
  [n]
  (->>
    (d/datoms @db/*conn* :avet :file/last-modified)
    reverse
    (take n)
    (map :e)))

(comment
  (count (last-modified-files->es 40))
  (->> (last-modified-files->es 40)
       sort
       (d/pull-many @db/*conn* '[*])
       count
       )
  (->> (d/datoms @db/*conn* :avet :file/last-modified) reverse)
  (->> (last-modified-files->es 5)
       (d/pull-many @db/*conn* '[*])))

(defn recent-wallpapers->es
  [n]
  (->>
    (d/datoms @db/*conn* :avet :wallpaper/last-time-set)
    reverse
    (take n)
    (map :e)))

(defn recent-events->es
  [n]
  (->>
    (d/datoms @db/*conn* :avet :event/timestamp)
    reverse
    (take n)
    (map :e)))

(defn datoms-for-frontend []
  (->>
    (concat
      (last-modified-files->es 200)
      (recent-wallpapers->es 20)
      (recent-events->es 100))
    dedupe
    (mapcat #(d/datoms @db/*conn* :eavt %)))

  ;; (d/schema @db/*conn*)
  #_(d/datoms @db/*conn* :eavt))

(comment

  (->>
    (d/datoms @db/*conn* :avet :file/last-modified)
    reverse
    (take 30)
    (map :e)
    count
    )

  (datoms-for-frontend))

(defsys ^:dynamic *db-stream*
  :start (s/stream 100000)
  :stop (s/close! *db-stream*))

(comment
  (sys/start! `*db-stream*)
  (sys/restart! `*db-stream*))

(defn push-to-fe-db [txs]
  (s/put! *db-stream* txs))

(defsys ^:dynamic *tx->fe-db*
  :start (do
           (log/info "Adding :tx->fe-db db listener")
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :tx->fe-db
             (fn [tx]
               #_(def tx tx)
               (try
                 (log/info "sending datoms to the frontend")
                 (push-to-fe-db (:tx-data tx))
                 (catch Exception e
                   (log/warn "Error in tx->fe-db db listener" e)
                   tx)))))
  :stop
  (try
    (log/debug "Removing :tx->fe-db db listener")
    (d/unlisten! db/*conn* :tx->fe-db)
    (catch Exception e
      (log/debug "err removing listener" e)
      nil)))

(defn start-tx->fe-listener []
  (sys/start! `*tx->fe-db*))

(defn stop-tx->fe-listener []
  (sys/stop! `*tx->fe-db*))
