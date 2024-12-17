(ns api.db
  (:require
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [db.core :as db]
   [garden.core :as garden]
   [datascript.core :as d]
   [taoensso.telemere :as log]
   [dates.tick :as dt]))

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
          ;; 'current' deprecated, delete soon
          ;; [?e :org/tags "current"]
          )
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

(defn last-n-dailies->es [n]
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

(defn all-todos->es
  ([] (all-todos->es nil))
  ([n]
   (cond->> (db/datoms :avet :doctor/type :type/todo)
     true (map :e)
     n    (take n))))

(defn all-notes->es
  ([] (all-notes->es nil))
  ([n]
   (cond->> (db/datoms :avet :doctor/type :type/note)
     true (map :e)
     n    (take n))))

(defn all-pomodoros->es
  ([] (all-pomodoros->es nil))
  ([n]
   (cond->> (db/datoms :avet :doctor/type :type/pomodoro)
     true (map :e)
     n    (take n))))

(defn last-modified-files->es [n]
  (->>
    (db/datoms :avet :file/last-modified)
    (sort-by :v dt/sort-latest-first)
    (map :e)
    dedupe
    (take n)))

(comment
  (count (last-modified-files->es 40))
  (->> (d/datoms @db/*conn* :avet :file/last-modified) reverse)
  (->> (last-modified-files->es 40)
       (d/pull-many @db/*conn* '[:doctor/type
                                 :db/id
                                 :file/last-modified
                                 :org/name-string
                                 :org/short-path])
       (take 3)))

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

(defn recent-screenshots->es
  [n]
  (->>
    (db/datoms :avet :doctor/type :type/screenshot)
    reverse
    (take n)
    (map :e)))

(defn recent-clips->es
  [n]
  (->>
    (db/datoms :avet :doctor/type :type/clip)
    reverse
    (take n)
    (map :e)))

(defn repos->es []
  (->>
    (db/datoms :avet :doctor/type :type/repo)
    (filter (comp #(= % "russmatney") :repo/user-name
                  #(d/entity @db/*conn* %)
                  :e))
    (map :e)
    dedupe))

(comment
  (->>
    (repos->es)
    (d/pull-many @db/*conn* '[*])
    )
  )

(defn chess-games->es []
  (->>
    (db/datoms :avet :doctor/type :type/lichess-game)
    (map :e)
    dedupe))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; initial data to send to frontend

(defn datoms-for-frontend []
  (->>
    (concat
      #_(all-todos->es)
      #_(all-notes->es)
      (all-pomodoros->es)
      (prioritized-todos->es)
      (current-todos->es)
      (last-n-dailies->es 14)
      (last-modified-files->es 200)
      (recent-wallpapers->es 20)
      (recent-events->es 300)
      (recent-clips->es 10)
      (recent-screenshots->es 10)
      (repos->es)
      (chess-games->es))
    distinct
    (mapcat #(d/datoms @db/*conn* :eavt %))))

(comment
  (count (all-notes->es))
  (->>
    (datoms-for-frontend)
    (map :e)
    dedupe
    (d/pull-many @db/*conn* '[:file/last-modified
                              :doctor/type
                              :org/name-string
                              :org/short-path])
    (filter (comp #{:type/note} :doctor/type))
    #_(sort-by :file/last-modified dt/sort-chrono)
    (take 5))

  (->>
    (recent-clips->es 10)
    (mapcat #(d/datoms @db/*conn* :eavt %))
    )

  )

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
    (log/log! {:data {:count (count txs)}} "Pushing txs to frontend")
    (s/put! *db-stream* txs)))

(defsys ^:dynamic *tx->fe-db*
  :start (do
           (log/log! "Adding :tx->fe-db db listener")
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :tx->fe-db
             (fn [tx]
               (try
                 (push-to-fe-db (:tx-data tx))
                 (catch Exception e
                   (log/log! :warn ["Error in tx->fe-db db listener" e])
                   tx)))))
  :stop
  (try
    (log/log! "Removing :tx->fe-db db listener")
    (d/unlisten! db/*conn* :tx->fe-db)
    (catch Exception e
      (log/log! ["err removing listener" e])
      nil)))

(defn start-tx->fe-listener []
  (sys/start! `*tx->fe-db*))

(defn stop-tx->fe-listener []
  (sys/stop! `*tx->fe-db*))
