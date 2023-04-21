(ns garden.db
  (:require
   [db.core :as db]
   [db.listeners :as db.listeners]
   [garden.core :as garden]
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [babashka.fs :as fs]
   [item.core :as item]
   [api.db :as api.db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ->db-item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fallback-id
  "Used as a backup for dumping unique org items into the db.core.

  Handling changing names/re-arranging org-files without the uuids is a bit hairy...
  We could 'retire'/'archive' all nodes per :org/source-file every time a
  source-file is ingested, then try to match-up/combine/re-link the missing ones
  when things move around...
  "
  [{:org/keys [source-file short-path name-string parent-name relative-index]}]
  (when (and (or short-path source-file) name-string)
    (str name-string " " relative-index " " parent-name " > " (or short-path source-file))))

(defn ensure-list [xs]
  (if (string? xs) [xs] xs))

(declare fetch-matching-db-item)

(defn garden-note->db-item
  [{:org/keys [id links-to parent-ids urls tags] :as item}]
  (let [fallback (fallback-id item)
        db-match (fetch-matching-db-item item)]
    (if-not (or id fallback)
      (log/info "Could not create fallback id for org item" (:org/name-string item))

      (cond-> item
        id (assoc :org/id id)

        (and fallback
             ;; might be the wrong move, but syncing/upsert conflicts are a headache rn
             (not id))
        (assoc :org/fallback-id fallback)

        db-match
        (assoc :db/id (:db/id db-match))

        (seq links-to)
        (assoc :org/links-to (->> links-to
                                  (map (fn [link]
                                         ;; build ref for what this refers to
                                         {:org/id        (:link/id link)
                                          :org/link-text (:link/text link)}))
                                  (into [])))

        (seq parent-ids)
        (assoc :org/parents (->> parent-ids
                                 (map (fn [parent-id]
                                        ;; build ref for what this refers to
                                        {:org/id parent-id}))
                                 (into [])))

        true ;; quick attempt to un-lazy some seqs
        (->> (map (fn [[k v]]
                    [k (if (coll? v) (->> v (into []))
                           v)]))
             (into {}))

        (:org/status item)       (assoc :doctor/type :type/todo)
        (not (:org/status item)) (assoc :doctor/type :type/note)

        true (assoc :org/urls (ensure-list urls))
        true (assoc :org/tags (ensure-list tags))

        ;; if this can be calced, assoc it
        (item/->latest-timestamp :type/note item)
        (assoc :event/timestamp (item/->latest-timestamp :type/note item))

        true
        (dissoc :org/body
                :org/items
                :org.prop/link-ids ;; old linking props
                :org.prop/begin-src ;; TODO proper source block handling
                :org.prop/end-src)))))

;; this should not be necessary
;; (defn other-db-updates
;;   [{:org/keys [links-to id parent-ids]}]
;;   ;; TODO if no `id`, we probably want to link to the nearest parent
;;   (when id
;;     (concat

;;       (->> links-to (map (fn [link]
;;                            ;; TODO consider creating a first-class link/edge entity?
;;                            {:org/id          (:link/id link)
;;                             :org/link-text   (:link/text link)
;;                             :org/linked-from id})))
;;       (->> parent-ids (map (fn [parent-id]
;;                              ;; TODO consider creating a first-class link/edge entity?
;;                              {:org/id        parent-id
;;                               :org/child-ids id}))))))


(defn -compare-db-notes
  "Helper for researching :org/fallback-id's implementation/implications."
  [notes]
  (let [db-note-fallback-ids (->> notes
                                  (map garden-note->db-item)
                                  (remove nil?)
                                  (map :org/fallback-id))]
    (if (= (count notes)
           (count (set db-note-fallback-ids)))
      {:same-count (count notes)}
      {:input-notes     (count notes)
       :db-notes        (count (set db-note-fallback-ids))
       :duped-fallbacks (->> db-note-fallback-ids (frequencies)
                             (remove (comp #{1} second))
                             )})))

(comment
  (-compare-db-notes (garden/paths->flattened-garden-notes
                       (garden/daily-paths 1)))
  (->>
    (-compare-db-notes (garden/all-garden-notes-flattened))
    :duped-fallbacks
    (remove (comp #(string/includes? % "archive") first)))

  (->> (garden/daily-paths 3)
       garden/paths->flattened-garden-notes
       (map garden-note->db-item)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sync
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -on-error-log-notes [txs]
  (doall
    (->> txs
         (map
           (fn [item]
             (log/info (str "\n" (:org/source-file item) " - " (:org/fallback-id item))
                       item))))) )

(comment
  (-on-error-log-notes []))

(defn sync-db-notes
  "`db-notes` are garden-notes already mapped via `garden-note->db-item`."
  [opts db-notes]
  (let [page-size (:page-size opts 5)]
    (->>
      db-notes
      (sort-by :org/source-file)
      (sort-by :org/fallback-id)
      (partition-all page-size)
      (map (fn transact-garden-notes [notes]
             (db/transact
               notes
               {:on-error -on-error-log-notes
                :verbose? true
                :on-unsupported-type
                (fn [note]
                  (log/debug "Unsupported type on note" note))
                ;; retry logic that narrows in on bad records
                ;; NOTE not ideal if we care about things belonging to the same transaction
                :on-retry
                (fn [notes]
                  (let [size (count notes)
                        half (/ size 2)]
                    (if (> size 1)
                      (do
                        (log/info "\n\nRetrying with smaller groups." (count notes))
                        (transact-garden-notes (->> notes (take half)))
                        (transact-garden-notes (->> notes (drop half) (take half))))
                      (log/info "\n\nProblemmatic record:" (some->>
                                                             notes first
                                                             :org/name-string)))))})))
      doall)))

(defn sync-garden-notes-to-db
  "Adds the passed garden notes to the db, after mapping them via `garden-note->db-item`."
  ([notes] (sync-garden-notes-to-db nil notes))
  ([opts garden-notes]
   (->> garden-notes
        (map garden-note->db-item)
        (sync-db-notes opts))))

(comment
  (let [x         [2 3 4 5 6 7 8]
        size      (count x)
        half-size (/ size 2)]
    (->> x (drop half-size) (take half-size))
    (->> x (take (/ 1 2)))))

(defn sync-garden-paths-to-db
  ([paths] (sync-garden-paths-to-db nil paths))
  ([opts paths]
   (->> paths
        garden/paths->flattened-garden-notes
        (sync-garden-notes-to-db opts))))

(defn sync-last-touched-garden-files
  ([] (sync-last-touched-garden-files {:n 50}))
  ([{:keys [n]}]
   (sync-garden-paths-to-db
     {:page-size 20}
     (->> (garden/last-modified-paths) (take n)))))

(defn sync-all-garden-files []
  (sync-garden-paths-to-db
    {:page-size 2000}
    ;; oldest first
    (->> (garden/last-modified-paths) reverse)))

(comment
  api.db/*tx->fe-db*
  (api.db/start-tx->fe-listener)
  (api.db/stop-tx->fe-listener)

  db.listeners/*garden->blog*
  (db.listeners/start-garden->blog-listener)
  (db.listeners/stop-garden->blog-listener)

  (sync-garden-paths-to-db
    {:page-size 20}
    (concat
      (garden/basic-todo-paths)
      (garden/daily-paths 30)))

  (sync-garden-notes-to-db
    {:page-size 2000}
    (->>
      ;; the big one!!
      (garden/all-garden-notes-flattened)
      ;; (drop 15000)
      ))

  (sync-garden-notes-to-db
    {:page-size 20}
    (->>
      (garden/all-garden-notes-flattened)
      (filter (comp #(string/includes? % "bb_cli") :org/source-file))))

  (sync-garden-paths-to-db
    {:page-size 20}
    (->> (garden/all-garden-notes-nested)
         (sort-by :file/last-modified)
         (reverse)
         (take 200)
         (map :org/source-file)))

  (count
    (garden/all-garden-notes-flattened)))

(declare fetch-notes-for-source-file)

(defn sync-and-purge-for-path [garden-path]
  (let [notes-to-sync
        (->> [garden-path]
             garden.core/paths->flattened-garden-notes
             ;; do we need this? remove children so they don't get synced early?
             ;; OR maybe don't flatten and rely on the children syncing?
             ;; (map #(dissoc % :org/items))
             (map garden-note->db-item))
        db-note-ids    (->> notes-to-sync (map :org/id) (into #{}))
        db-note-fb-ids (->> notes-to-sync (map :org/fallback-id) (into #{}))]

    (sync-db-notes {:page-size 200} notes-to-sync)

    ;; retract notes with this source-file that are not in db-notes
    (let [all-db-notes   (fetch-notes-for-source-file garden-path)
          notes-to-purge (->> all-db-notes
                              (remove (fn [db-note]
                                        (or (and (:org/id db-note)
                                                 (db-note-ids (:org/id db-note)))
                                            (and (:org/fallback-id db-note)
                                                 (db-note-fb-ids (:org/fallback-id db-note)))))))]
      (when (seq notes-to-purge)
        (log/info "Purging" (count notes-to-purge) "/" (count all-db-notes) "notes from the db"
                  (->> notes-to-purge (map :org/name-string))))
      (->> notes-to-purge
           (map :db/id)
           (remove nil?)
           (db/retract))

      ;; TODO also need to purge removed tags, changed parent-names... probably all lists
      )))

(comment
  (def note
    (->>
      (garden/all-garden-notes-flattened)
      (filter (comp #(string/includes? % "clawe doctor org dedup") :org/name-string))
      first))

  (sync-and-purge-for-path (:org/source-file note))

  (-> (garden/daily-path 1)
      sync-and-purge-for-path))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-db-garden-notes []
  (->>
    (db/query '[:find (pull ?e [*])
                :where [?e :doctor/type :type/note]])
    (map first)))

(comment
  (count (fetch-db-garden-notes))
  (->>
    (fetch-db-garden-notes)
    (map :org/source-file)
    dedupe
    count))

(defn fetch-notes-for-source-file [source-file]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?source-file
                :where [?e :org/source-file ?source-file]
                ]
              (str source-file))
    (map first)))

(comment
  (def note
    (->>
      (garden/all-garden-notes-flattened)
      (filter (comp #(string/includes? % "clawe doctor org dedup") :org/name-string))
      first))
  (->>
    (fetch-notes-for-source-file (:org/source-file note))
    (map :org/name-string)
    sort))

(defn notes-with-tags [tags]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?tags
                :where
                [?e :doctor/type :type/note]
                [?e :org/tags ?tag]
                [(?tags ?tag)]]
              tags)
    (map first)))

(comment
  (count (garden/all-garden-notes-flattened))
  (count (fetch-db-garden-notes))

  (notes-with-tags #{"post"})

  (->>
    (db/query '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/note]
                [?e :org.prop/archive-time ?atime]])
    (map first)
    (map :org/source-file))

  ;; delete notes
  (->>
    (db/query '[:find ?e
                :where
                [?e :doctor/type :type/note]
                [?e :org/source-file ?file]
                [(string/includes? ?file "/archive/")]
                ])
    (map first)
    ;; (partition-all 200)
    ;; (map db/retract)
    ;; (doall)
    ;; count
    )

  (->>
    (db/query '[:find ?e
                :where
                [?e :org/source-file ?file]
                [(string/includes? ?file "conflicted copy")]])
    (map first)
    (db/retract))

  (->>
    (db/query '[:find (pull ?e [:org/urls])
                :where
                [?e :org/urls ?urls]])
    (map first)
    (sort-by (comp count :org/urls) >)
    (take 5)))

(defn fetch-matching-db-item
  "Attempts to fetch a db item using the passed org item."
  [item]
  (let [id    (:org/id item)
        fb-id (fallback-id item)
        matches
        (->>
          (db/query '[:find (pull ?e [*])
                      :in $ ?id ?fb-id
                      :where
                      (or
                        [?e :org/id ?id]
                        [?e :org/fallback-id ?fb-id])]
                    id fb-id)
          (map first))]
    (when (> (count matches) 1)
      (log/info "Multiple matches found for org item" id fb-id (->> matches (map :db/id))))

    (some->> matches first)))

(comment
  (->>
    (garden/daily-paths 1)
    (garden/paths->nested-garden-notes)
    #_ (filter (comp #(string/includes? % "clawe hacking") :org/name-string))
    first
    fetch-matching-db-item)

  (first (fetch-db-garden-notes))
  (def note
    (->>
      (garden/all-garden-notes-flattened)
      (filter (comp #(string/includes? % "clawe doctor org dedup") :org/name-string))
      first))
  (fallback-id note)
  (fetch-matching-db-item note))

(defn merge-db-item
  "Attempts to fetch and merge a db item using the passed org item."
  [item]
  (let [match (fetch-matching-db-item item)]
    (merge match item)))

(comment
  (def note
    #_(first (fetch-db-garden-notes))
    (->>
      (garden/all-garden-notes-flattened)
      (filter (comp #(string/includes? % "clawe doctor org dedup") :org/name-string))
      first))
  (merge-db-item note))
