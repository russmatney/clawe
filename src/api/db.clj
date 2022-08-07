(ns api.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [defthing.db :as db]
   [datascript.core :as d]
   [taoensso.timbre :as log]))


(defn datoms-for-frontend []
  (d/datoms @db/*conn* :eavt))

(defsys *db-stream*
  :start (s/stream)
  :stop (s/close! *db-stream*))

(comment
  (sys/start! `*db-stream*))

(defn update-stream [txs]
  (s/put! *db-stream* txs))

(defsys *tx->fe-db*
  :start (do
           (log/info "Adding :tx->fe-db db listener")
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :tx->fe-db
             (fn [tx]
               #_(def tx tx)
               (try
                 (log/info "garden note transacted!" tx)
                 (update-stream (:tx-data tx))
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
