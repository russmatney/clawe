(ns garden.core
  (:require
   [babashka.fs :as fs]
   [manifold.stream :as s]
   [org-crud.core :as org-crud]
   [systemic.core :refer [defsys] :as sys]
   [ralphie.zsh :as r.zsh]
   [util]
   [dates.tick :as dates.tick]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org file paths
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO quite alot of zsh/expand in here
;; probably lots of room to improve performance when collecting all these files
;; we need a monad!

;; daily

(defn daily-path
  "Returns a path for the passed day. Defaults to today."
  ([] (daily-path (first (dates.tick/days 1))))
  ([day]
   (->
     (str "~/todo/daily/" day ".org")
     r.zsh/expand)))

(comment
  (daily-path))

(defn daily-paths
  ([] [(daily-path)])
  ([n] (->> (dates.tick/days n) (map daily-path))))

(comment
  (daily-paths)
  (daily-paths 6))

(defn all-daily-paths []
  (r.zsh/expand-many "~/todo/daily/*.org"))

;; monthly archive

(defn monthly-archive-path
  "Returns a path to the org archive for the passed month.
  Defaults to this month."
  ([] (monthly-archive-path (first (dates.tick/months 1))))
  ([year-month]
   (->
     (str "~/todo/archive/" year-month ".org")
     r.zsh/expand)))

(comment
  (monthly-archive-path))

(defn monthly-archive-paths
  ([] [(monthly-archive-path)])
  ([n] (->> (dates.tick/months n) (map monthly-archive-path))))

(comment
  (monthly-archive-paths)
  (monthly-archive-paths 6))

(defn all-monthly-archive-paths []
  (r.zsh/expand-many "~/todo/archive/*.org"))

;; todos

(defn basic-todo-paths []
  (-> "~/todo/{journal,projects,icebox}.org" r.zsh/expand-many))

(defn repo-todo-paths [repo-ids]
  (->> repo-ids
       (map #(str "~/" % "/{readme,todo}.org"))
       (mapcat r.zsh/expand-many)))

(comment
  (repo-todo-paths #{"russmatney/clawe" "teknql/fabb" "russmatney/dino" "doesnot/exist"}))

;; workspaces

(defn workspace-paths []
  ;; could find matches in non-workspace dir same-root paths
  (-> "~/todo/garden/workspaces/*.org" r.zsh/expand-many))

;; garden

(defn flat-garden-paths
  "Paths to files in ~/todo/garden/*.org"
  []
  (-> "~/todo/garden/*.org" r.zsh/expand-many))

(comment
  (flat-garden-paths))

;; all of these

(defn all-garden-paths
  "All of them!

  Well, kind of: a few from ~/todo/ via (basic-todo-paths), all the dailies and archives,
  the whole garden, and all the workspace-garden files.
  "
  []
  (concat
    (basic-todo-paths)
    (all-daily-paths)
    (workspace-paths)
    (flat-garden-paths)
    (all-monthly-archive-paths)))

(comment
  (count
    (all-garden-paths))
  )

;; general helper

(defn org-file-paths
  "Helper for getting a list of org Files. Ensures they all exist.
  Defaults to some recent dailies, journal.org, projects.org."
  ([] (org-file-paths (concat (basic-todo-paths) (daily-paths 3))))
  ([org-paths] (->> org-paths (map fs/file)
                    ;; b/c we project out dailies, we might be missing some of these
                    ;; so this removes files that don't exist
                    (filter fs/exists?))))

(comment
  (org-file-paths)
  (org-file-paths (repo-todo-paths #{"russmatney/clawe" "russmatney/org-crud" "doesnot-exist"}))
  (org-file-paths (daily-paths 14))

  (count
    (org-file-paths
      (all-garden-paths))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-last-modified
  [item]
  (-> item :org/source-file fs/last-modified-time str))

(defn org->garden-note
  [{:org/keys      [source-file]
    :org.prop/keys [title created-at]
    :as            item}]
  (let [last-modified (get-last-modified item)]
    (->
      item
      (assoc :garden/file-name (fs/file-name source-file)
             :org/short-path (str (-> source-file fs/parent fs/file-name)
                                  "/" (fs/file-name source-file))
             :org.prop/created-at created-at
             :org.prop/title (or title (fs/file-name source-file))
             :file/last-modified last-modified))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; paths -> org items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn paths->nested-garden-notes [paths]
  (->> paths
       org-file-paths
       (map org-crud/path->nested-item)
       (map org->garden-note)))

(comment
  (paths->nested-garden-notes (daily-paths)))

(defn paths->flattened-garden-notes [paths]
  (->> paths
       org-file-paths
       (mapcat org-crud/path->flattened-items)
       (map org->garden-note)))

(comment
  (paths->flattened-garden-notes (daily-paths)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; all-garden-notes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO wtf is this?
(defn all-garden-notes-nested
  "All relevant org items from the garden, nested
  (a note per file, with nested headline as children)."
  []
  (->>
    (org-file-paths
      (all-garden-paths))
    (map org-crud/path->nested-item)
    (map org->garden-note)))

(defn all-garden-notes-flattened
  "All relevant org items from the garden, flattened (a note per headline)."
  []
  (->>
    (org-file-paths
      (all-garden-paths))
    (mapcat org-crud/path->flattened-items)
    (map org->garden-note)))

(comment
  (->>
    (all-garden-notes-nested)
    (take 3))

  (->>
    (all-garden-notes-flattened)
    (take 3))

  (count
    (all-garden-notes-flattened))

  (->> (all-garden-notes-flattened)
       (remove (comp uuid? :org/id))
       (map :org/level)
       (frequencies)
       )
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; journal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-journals
  "Returns currently active journal notes.

  - journal.org
  - projects.org
  - dailies for the last 3 days (today inclusive)
  "
  []
  (->>
    (paths->nested-garden-notes
      (concat
        (daily-paths 3)
        (basic-todo-paths)))
    (sort-by :org/source-file)
    (map util/drop-complex-types)))

(comment
  (active-journals))

(defsys *journals-stream*
  :start (s/stream)
  :stop (s/close! *journals-stream*))

(defn update-journals []
  (s/put! *journals-stream* (active-journals)))

(comment
  (update-journals))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-garden []
  (->>
    (all-garden-notes-nested)
    (map util/drop-complex-types)))

(comment
  (->>
    (get-garden)
    (count)))

(defsys *garden-stream*
  :start (s/stream)
  :stop (s/close! *garden-stream*))

(comment
  (sys/start! `*garden-stream*))

(defn update-garden []
  (s/put! *garden-stream* (get-garden)))

(comment
  (update-garden))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-full-item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn full-item
  [{:org/keys [source-file]}]
  (org-crud/path->nested-item source-file))
