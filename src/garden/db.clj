(ns garden.db
  (:require
   [db.core :as db]
   [garden.core :as garden]
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [item.core :as item]))

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
  [{:org/keys [short-path name parent-name relative-index]}]
  (when (and short-path name)
    (str name " " relative-index " " parent-name " > " short-path)))

(defn garden-note->db-item
  [{:org/keys [id links-to parent-ids] :as item}]
  (let [fallback (fallback-id item)]
    (if (or id fallback)
      (cond-> item
        id (assoc :org/id id)

        fallback
        (assoc :org/fallback-id fallback)

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
                                 (into [])
                                 ))

        true
        (->>
          ;; quick attempt to un-lazy some seqs
          (map (fn [[k v]]
                 [k (if (coll? v) (->> v (into []))
                        v)]))
          (into {}))


        true
        (assoc :doctor/type :type/garden)

        ;; if this can be calced...
        (item/->latest-timestamp
          ;; kind of annoying item depends on this type
          (assoc item :doctor/type :type/garden))
        ;; then assoc it
        (assoc :event/timestamp
               (item/->latest-timestamp
                 ;; kind of annoying item depends on this type
                 (assoc item :doctor/type :type/garden)))

        true
        (dissoc :org/body
                :org/items
                :org.prop/link-ids ;; old linking props
                :org.prop/begin-src ;; TODO proper source block handling
                :org.prop/end-src
                ))
      (log/info "Could not create fallback id for org item" item))))

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

(defn sync-garden-notes-to-db
  "Adds the passed garden notes to the db, after mapping them via `garden-note->db-item`."
  ([notes] (sync-garden-notes-to-db nil notes))
  ([opts garden-notes]
   (let [page-size (:page-size opts 5)]
     (doall
       (->>
         garden-notes
         (map garden-note->db-item)
         ;; (mapcat (fn [item]
         ;;           (concat [item] (other-db-updates item))))
         (sort-by :org/source-file)
         (sort-by :org/fallback-id)
         (partition-all page-size)
         (map (fn transact-garden-notes [notes]
                (db/transact
                  notes
                  {:on-error -on-error-log-notes
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
                           (log/info "Retrying with smaller groups." (count notes))
                           (transact-garden-notes (->> notes (take half)))
                           (transact-garden-notes (->> notes (drop half) (take half))))
                         (log/info "Problemmatic record:" notes))))}))))))))

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

(comment
  (sync-garden-paths-to-db
    {:page-size 20}
    (concat
      (garden/basic-todo-paths)
      (garden/daily-paths 30)))

  (sync-garden-notes-to-db
    {:page-size 20}
    (->>
      ;; the big one!!
      (garden/all-garden-notes-flattened)
      ;; (drop 15000)
      ))

  (sync-garden-paths-to-db
    {:page-size 20}
    (->> (garden/all-garden-notes-nested)
         (sort-by :file/last-modified)
         (reverse)
         (take 200)
         (map :org/source-file)))

  (count
    (garden/all-garden-notes-flattened)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-db-garden-notes
  []
  (->>
    (db/query '[:find (pull ?e [*])
                :where [?e :doctor/type :type/garden]])
    (map first)))

(defn notes-with-tags [tags]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?tags
                :where
                [?e :doctor/type :type/garden]
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
                [?e :doctor/type :type/garden]
                [?e :org.prop/archive-time ?atime]])
    (map first)
    (map :org/source-file))

  ;; delete notes
  (->>
    (db/query '[:find ?e
                :where
                [?e :doctor/type :type/garden]
                [?e :org/source-file ?file]
                [(string/includes? ?file "/archive/")]
                ])
    (map first)
    ;; (partition-all 200)
    ;; (map db/retract)
    ;; (doall)
    ;; count
    ))
