(ns db.listeners
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as d]

   [db.core :as db]
   [item.core :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; expo consumer
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *garden->expo*
  :start (do
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :garden->expo
             (fn [tx]
               (try
                 ;; TODO disabled for now - will re-think when approaching expo again
                 ;; (log/info "garden note transacted!")
                 ;; (expo/update-posts)
                 (catch Exception e
                   (log/warn "Error in garden->expo db listener" e)
                   tx)))))
  :stop
  (try
    (log/debug "Removing :garden->expo db listener")
    (d/unlisten! db/*conn* :garden->expo)
    (catch Exception e
      (log/debug "err removing listener" e)
      nil)))

;; (defn start-garden->expo-listener []
;;   (sys/start! `*garden->expo*))

;; (defn stop-garden->expo-listener []
;;   (sys/stop! `*garden->expo*))

(comment
  (sys/start! `*garden->expo*)
  (sys/stop! `*garden->expo*)

  (d/db db/*conn*)

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data expander
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ensure-timestamps [tx]
  (->> tx
       :tx-data
       (map :e) ;; dat entity
       (into #{}))
  (let [ent-updates
        (->> tx
             :tx-data
             (map :e) ;; dat entity
             (into #{})
             (d/pull-many @db/*conn* '[*])
             (remove :event/timestamp)
             (map (fn [ent]
                    (assoc ent :event/timestamp (item/->latest-timestamp ent))))
             ;; only transact ents that recieved this
             (filter :event/timestamp))]
    (when (> (count ent-updates) 0)
      (log/info "[DB] Adding :event/timestamp to " (count ent-updates) " records")
      (db/transact ent-updates))))

(defsys *data-expander*
  :start
  (log/info "Adding *data-expander* db listener")
  (sys/start! `db/*conn*)
  (d/listen!
    db/*conn* :data-expander
    (fn [tx]
      (try
        (ensure-timestamps tx)
        (catch Exception e
          (log/warn "Error in *data-expander* db listener" e)
          tx))))
  :stop
  (try
    (log/debug "Removing *data-expander* db listener")
    (d/unlisten! db/*conn* :data-expander)
    (catch Exception e
      (log/debug "err removing listener" e)
      nil)))

(comment
  (when (sys/running? `*data-expander*)
    (sys/restart! `*data-expander*))

  (->>
    (db/query '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/commit]
                ])
    (map first)
    (filter :event/timestamp)
    first
    #_item/->latest-timestamp
    #_((fn [ent]
         (assoc ent :user/touched "double true")))
    #_db/transact
    )

  (do
    (db/transact [{:some-random-data "hi there"
                   :some/names       "paced key"}])
    nil)
  )
