(ns api.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [db.core :as db]
   [garden.core :as garden]
   [datascript.core :as d]
   [taoensso.timbre :as log]))

(defn prioritized-todos->es []
  (->>
    (db/query
      '[:find ?e
        :where
        [?e :doctor/type :type/todo]
        [?e :org/priority _]])
    (map first)))

(defn current-todos->es []
  (->>
    (db/query
      '[:find ?e
        :where
        [?e :doctor/type :type/todo]
        (or
          [?e :org/status :status/in-progress]
          [?e :org/tags "current"])
        ;; filter out todos with lingering 'current' tags but completed statuses
        (not
          [?e :org/status ?status]
          [(contains? #{:status/done
                        :status/skipped
                        :status/cancelled} ?status)])])
    ;; TODO include their children? maybe just pass all todos?
    (map first)))

(comment
  (count (current-todos->es)))

(defn last-n-dailies->es
  [n]
  (->>
    (db/query '[:find ?e
                :in $ ?paths
                :where
                [?e :org/source-file ?f]
                [(contains? ?paths ?f)]]
              (->> n garden/daily-paths (into #{})))
    (map first)))

(comment
  (count (last-n-dailies->es 7)))

(defn last-modified-files->es
  [n]
  (->>
    (db/datoms :avet :file/last-modified)
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
    (db/datoms :avet :wallpaper/last-time-set)
    reverse
    (take n)
    (map :e)))

(defn recent-events->es
  [n]
  (->>
    (db/datoms :avet :event/timestamp)
    reverse
    (take n)
    (map :e)))

(defn repos->es []
  (->>
    (db/datoms :avet :doctor/type :type/repo)
    (filter (comp #(= % "russmatney") :repo/user-name
                  #(d/entity @db/*conn* %)
                  :e))
    (map :e)))

(defn chess-games->es []
  (->>
    (db/datoms :avet :doctor/type :type/lichess-game)
    (map :e)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; initial data to send to frontend

(defn datoms-for-frontend []
  (->>
    (concat
      (prioritized-todos->es)
      (current-todos->es)
      (last-n-dailies->es 14)
      (last-modified-files->es 200)
      (recent-wallpapers->es 20)
      (recent-events->es 300)
      (repos->es)
      (chess-games->es))
    distinct
    (mapcat #(d/datoms @db/*conn* :eavt %))))

(comment
  (->>
    (d/datoms @db/*conn* :avet :file/last-modified)
    reverse
    (take 30)
    (map :e)
    count)

  (datoms-for-frontend))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db stream and frontend data push

(defsys ^:dynamic *db-stream*
  :start (s/stream 100000)
  :stop (s/close! *db-stream*))

(comment
  (sys/start! `*db-stream*)
  (sys/restart! `*db-stream*))

(defn push-to-fe-db [txs]
  (when (seq txs)
    (log/debug "Pushing txs to frontend")
    (s/put! *db-stream* txs)))

(defsys ^:dynamic *tx->fe-db*
  :start (do
           (log/info "Adding :tx->fe-db db listener")
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :tx->fe-db
             (fn [tx]
               (try
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
