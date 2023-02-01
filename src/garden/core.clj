(ns garden.core
  (:require
   [babashka.fs :as fs]
   [org-crud.core :as org-crud]
   [ralphie.zsh :as r.zsh]
   [util]
   [dates.tick :as dates.tick]
   [db.core :as db]
   [wing.core :as w]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org file paths
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; daily

(defn daily-path
  "Returns a path for the passed day. Defaults to today.
  If `day` is passed as an int, the daily path from `day` days ago is used.
  If `day` is a string, it is assumed to be the desired `YYYY-MM-DD` file name."
  ([] (daily-path (first (dates.tick/days 1))))
  ([day]
   (let [day  (cond (int? day) (first (dates.tick/days (+ day 1)))
                    :else      day)
         path (str (fs/home) "/todo/daily/" day ".org")]
     (when (fs/exists? path) path))))

(comment
  (daily-path)
  (daily-path 1)
  (daily-path 2)
  (dates.tick/days 3))

(defn daily-paths
  ([] [(daily-path)])
  ([n] (->> (dates.tick/days n) (map daily-path) (remove nil?))))

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
   (str (fs/home) "/todo/archive/" year-month ".org")))

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

(comment
  (all-monthly-archive-paths))

;; todos

(defn basic-todo-paths []
  (-> "~/todo/{journal,projects}.org" r.zsh/expand-many)
  ;; (-> "~/todo/{journal,projects,icebox}.org" r.zsh/expand-many)
  )

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
  "All of the ones i care to ingest, anyway.

  - basic todo paths (journal, projects, icebox)
  - daily/*
  - garden/*
  - garden/workspaces/*
  "
  []
  (concat
    (basic-todo-paths)
    (all-daily-paths)
    (workspace-paths)
    (flat-garden-paths)))

(comment
  (count
    (all-garden-paths)))

(defn last-modified-paths []
  (->> (all-garden-paths)
       (filter fs/exists?)
       (map (fn [p]
              {:path     p
               :last-mod (fs/last-modified-time p)}))
       (sort-by :last-mod)
       reverse
       (map :path)))

;; general helper

(defn org-file-paths
  "Helper for getting a list of org Files. Ensures they all exist.
  Defaults to some recent dailies, journal.org, projects.org."
  ([] (org-file-paths (concat (basic-todo-paths) (daily-paths 3))))
  ([org-paths] (->> org-paths (map fs/file)
                    ;; b/c we project out dailies, we might be missing some of these
                    ;; so this removes files that don't exist
                    (filter fs/exists?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; paths -> org items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn paths->nested-garden-notes [paths]
  (->> paths
       org-file-paths
       (map org-crud/path->nested-item)))

(comment
  (paths->nested-garden-notes (daily-paths)))

(defn paths->flattened-garden-notes [paths]
  (->> paths
       org-file-paths
       (mapcat org-crud/path->flattened-items)))

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
    (all-garden-paths)
    (org-file-paths)
    (map org-crud/path->nested-item)
    (remove nil?)))

(defn all-garden-notes-flattened
  "All relevant org items from the garden, flattened (a note per headline)."
  []
  (->>
    (all-garden-paths)
    (org-file-paths)
    (mapcat org-crud/path->flattened-items)
    (remove nil?)))

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
       (frequencies)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-full-item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn full-item
  [opts]
  (let [source-file (:org/source-file opts)
        id          (:org/id opts)
        source-file (cond
                      source-file
                      source-file

                      id
                      (->
                        (db/query '[:find [(pull ?e [:org/source-file])]
                                    :in $ ?id
                                    :where
                                    [?e :org/id ?id]]
                                  (util/ensure-uuid id))
                        first
                        :org/source-file))]
    (if source-file
      (org-crud/path->nested-item source-file)

      ;; TODO in this case, get the source-file from the org-roam db and ingest it
      ;; we may not have a source-file if we're requesting a full-item with some partial data
      ;; (e.g. a link-id)
      (println "Could not find source-file for opts" opts))))

(comment
  (db/query '[:find [(pull ?e [:org/source-file])]
              :in $ ?id
              :where
              [?e :org/id ?id]] #uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F")
  (full-item {:org/id "notid"})
  (full-item {:org/id "3a89063f-ef16-4156-9858-fc941b448057"})
  (full-item {:org/id #uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; recently modified org
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn last-modified-org-paths []
  (->>
    (db/query '[:find (pull ?e [:org/source-file :file/last-modified])
                :where
                [?e :org/source-file ?f]])
    (map first)
    (remove (comp #(re-seq #"/archive/" %) :org/source-file))
    (w/distinct-by :org/source-file)
    (sort-by :file/last-modified)
    reverse))

(comment
  (->>
    (last-modified-org-paths)
    count
    #_(take 3)
    )
  )
