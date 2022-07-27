(ns garden.db
  (:require [defthing.db :as db]
            [garden.core :as garden]
            [clojure.string :as string]))

(defn default-garden-sync-notes
  "Notes for the last 7 dailies."
  []
  (garden/paths->flattened-garden-notes
    (garden/daily-paths 7)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ->db-item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fallback-id
  "Used as a backup for dumping org items into the defthing.db.

  There are some gotchas - same-name items will clobber each other.
  For now you can add an org id to items with `org-id-get-create`.

  Could upgrade org-crud to provide a bit more context to improve this fallback
  ;; TODO include #, as in which child am i?
  ;; TODO include parents-names (all names to root)

  Handling changing names/re-arranging org-files without the uuids is a hairy...
  We could 'retire'/'archive' all nodes per :org/source-file every time a
  source-file is ingested, then try to match up/combine/re-link the missing ones
  when things move around...
  "
  [{:org/keys [short-path name word-count]}]
  (when (and short-path name)
    (str name " " word-count " > " short-path)))

(defn garden-note->db-item
  [{:org/keys [id] :as item}]
  (let [fallback (fallback-id item)]
    (if (or id fallback)
      (cond-> item
        id (assoc :org/id (if (uuid? id) id (java.util.UUID/fromString id)))

        fallback
        (assoc :org/fallback-id fallback)

        true
        (assoc :doctor/type :type/garden))
      (println "Could not create fallback id for org item" item))))


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
  (-compare-db-notes (default-garden-sync-notes))
  (-compare-db-notes (garden/paths->flattened-garden-notes
                       (garden/daily-paths 1)))
  (->>
    (-compare-db-notes (garden/all-garden-notes-flattened))
    :duped-fallbacks
    (remove (comp #(string/includes? % "archive") first)))

  (->> (default-garden-sync-notes) (map garden-note->db-item)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sync
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sync-garden-notes-to-db
  "Adds the passed garden notes to the db, after mapping them via `garden-note->db-item`.

  Defaults to whatever `default-garden-sync-notes` indicates."
  ([] (sync-garden-notes-to-db nil))
  ([garden-notes]
   (let [garden-notes (or garden-notes (default-garden-sync-notes))]
     (->>
       garden-notes
       (map garden-note->db-item)
       (db/transact)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-db-garden-notes
  []
  (db/query '[:find (pull ?e [*])
              :where [?e :doctor/type :type/garden]]))

(comment
  (sync-garden-notes-to-db)

  5
  (fetch-db-garden-notes))
