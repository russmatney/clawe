(ns db.listeners
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as d]

   [blog.publish :as blog.publish]
   [blog.db :as blog.db]

   ;; circular dep!!
   #_[blog.core :as blog]
   [db.core :as db]
   [item.core :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data expander
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ensure-timestamps [tx]
  (let [ent-updates
        (->> tx
             :tx-data
             (map :e)
             (into #{})
             (d/pull-many @db/*conn* '[*])
             (remove :event/timestamp)
             (map (fn [ent]
                    (assoc ent :event/timestamp (item/->latest-timestamp ent))))
             ;; only transact ents that recieved this
             (filter :event/timestamp)
             (map (fn [ent] (select-keys ent [:db/id :event/timestamp]))))]
    (when (> (count ent-updates) 0)
      (log/info "[DB] Adding :event/timestamp to " (count ent-updates) " records")
      (db/transact ent-updates))))

(comment
  ;; reingest with this running per entity before transacting
  (declare transact)

  ;; ents without timestamp?
  (->>
    (d/datoms @db/*conn* :eavt)
    (map :e)
    distinct
    (partition-all 200)
    (mapcat #(d/pull-many @db/*conn* '[*] %))
    (remove :event/timestamp)
    #_(map count))

  ;; add ts to ents missing timestamp
  (->>
    (d/datoms @db/*conn* :eavt)
    (map :e)
    (distinct)
    (partition-all 200)
    (map #(d/pull-many @db/*conn* '[*] %))
    (mapcat (fn [ent-group]
              (->> ent-group
                   (remove :event/timestamp)
                   (map (fn [ent]
                          (if-let [ts (item/->latest-timestamp ent)]
                            (do
                              (def ent ent)
                              (item/->latest-timestamp ent)
                              (assoc ent :event/timestamp ts))
                            ent)))
                   (filter :event/timestamp))))
    (remove nil?)
    (partition-all 200)
    #_(map count)
    (map (fn [ent-group]
           (def ent-group ent-group)
           (println "updating ents" (count ent-group))
           (db/transact ent-group)))
    )

  )

(defsys ^:dynamic *data-expander*
  :start
  (log/info "Adding *data-expander* db listener")
  (sys/start! `db/*conn*)
  (d/listen!
    db/*conn* :data-expander
    (fn [tx]
      (try
        (ensure-timestamps tx)
        ;; TODO support :ingested-at (or is this just tx-id)?
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
                [?e :doctor/type :type/commit]])
    (map first)
    (remove :event/timestamp)
    first
    #_item/->latest-timestamp
    #_((fn [ent]
         (assoc ent :user/touched "double true")))
    #_db/transact
    )

  (do
    (db/transact [{:some-random-data "hi there"
                   :some/names       "paced key"}])
    nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; blog re-render
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tx->republish-updated-note [tx]
  (let [txed-ents (->> tx :tx-data (map :e) (into #{})
                       (d/pull-many @db/*conn* '[*]))

        published-notes (->> txed-ents
                             (filter (comp blog.db/published-id? :org/id)))]

    (when (seq published-notes)
      (log/info "rerendering edited notes!"
                (->> txed-ents (map :org/name-string)))
      (->> published-notes (map blog.db/update-db-note) doall)
      (->> published-notes
           ;; NOTE CAREFUL! this is an easy way to accidentally publish notes!
           ;; publish-note also guards against it
           (map (comp blog.publish/publish-note :org/source-file))
           doall))))

(defsys ^:dynamic *garden->blog*
  :start
  (do
    (sys/start! `db/*conn*)
    (d/listen!
      db/*conn* :garden->blog
      (fn [tx]
        (try
          ;; TODO debouncing logic for this!!!
          (tx->republish-updated-note tx)

          (catch Exception e
            (log/warn "Error in garden->blog db listener" e)
            tx)))))
  :stop
  (try
    (log/debug "Removing :garden->blog db listener")
    (d/unlisten! db/*conn* :garden->blog)
    (catch Exception e
      (log/debug "err removing listener" e)
      nil)))

(defn start-garden->blog-listener []
  (sys/start! `*garden->blog*))

(defn stop-garden->blog-listener []
  (sys/stop! `*garden->blog*))

(comment
  (sys/start! `*garden->blog*)
  (sys/stop! `*garden->blog*)

  (d/db db/*conn*))
