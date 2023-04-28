(ns garden.core
  (:require
   [taoensso.timbre :as log]
   [babashka.fs :as fs]
   [org-crud.core :as org-crud]
   [org-crud.update :as org-crud.update]
   [ralphie.zsh :as r.zsh]
   [util]
   [clojure.string :as string]
   [dates.tick :as dates.tick]
   [db.core :as db]
   [wing.core :as w]
   [ralphie.git :as r.git]
   [tick.core :as t]))

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
  (-> "~/todo/{icebox}.org" r.zsh/expand-many))

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
  "
  []
  (concat
    (basic-todo-paths)
    (all-daily-paths)
    (flat-garden-paths)))

(comment
  (count (all-garden-paths)))

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
;; date parse helper

(def org-item-date-keys
  #{:org/closed
    :org/scheduled
    :org/deadline
    :org.prop/archive-time
    :org.prop/created-at
    :file/last-modified})

(defn item->parsed-date-fields [item]
  (->> org-item-date-keys
       (map (fn [k] [k (some-> item k dates.tick/parse-time-string)]))
       (remove (comp nil? second))
       (into {})))

(defn merge-parsed-datetimes [item]
  ;; TODO recursively apply to item children
  (merge item (item->parsed-date-fields item)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; paths -> org items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->nested-item
  "Wraps org-crud/path->nested-item, applying date parsing."
  [path]
  (-> path
      org-crud/path->nested-item
      ;; TODO consider/test performance hit
      merge-parsed-datetimes))

(defn path->flattened-items
  "Wraps org-crud/path->flattened-items, applying date parsing."
  [path]
  (->> path
       org-crud/path->flattened-items
       ;; TODO consider/test performance hit
       (map merge-parsed-datetimes)
       (remove nil?)
       (remove empty?)))

(defn paths->nested-garden-notes [paths]
  (->> paths org-file-paths
       (map path->nested-item)
       (remove nil?)
       (remove empty?)))

(comment
  (paths->nested-garden-notes (daily-paths)))

(defn paths->flattened-garden-notes [paths]
  (->> paths org-file-paths
       (mapcat path->flattened-items)
       (remove nil?)
       (remove empty?)))

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
    (map path->nested-item)
    (remove nil?)
    (remove empty?)))

(defn all-garden-notes-flattened
  "All relevant org items from the garden, flattened (a note per headline)."
  []
  (->>
    (all-garden-paths)
    (org-file-paths)
    (mapcat path->flattened-items)
    (remove nil?)
    (remove empty?)))

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
      (path->nested-item source-file)

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
    #_(take 3)))


(comment
  (->>
    "~/todo/garden/websites/*.org"
    r.zsh/expand-many
    (map path->nested-item)
    (remove nil?)
    (map (fn [website-note]
           (str (:org/name-string website-note)
                "\n\n" (:org/body-string website-note))))
    (string/join "\n\n--\n\n")
    (spit (str (fs/home) "/todo/garden/old_website_notes.org")))

  (path->nested-item
    (str (fs/expand-home "~/todo/garden/this_roam_linked_nodes_ui_is_backwards.org")))

  (slurp
    (str (fs/expand-home "~/todo/garden/this_roam_linked_nodes_ui_is_backwards.org"))))

;; TODO move to ralphie.git namespaces
(defn first-commit-dt [path]
  (-> path
      fs/expand-home
      (#(r.git/commits {:path % :n 1 :oldest-first true}))
      first
      :commit/author-date
      dates.tick/parse-time-string))

(comment
  (-> "~/todo/garden/dino.org" fs/expand-home)
  (-> "~/todo/garden/dino.org" first-commit-dt)
  (-> "~/todo/garden/dino.org" fs/expand-home fs/expand-home fs/parent str)
  )

;; TODO move to ralphie.git namespaces
(defn last-commit-dt [path]
  (-> path
      fs/expand-home
      (#(r.git/commits {:path % :n 1}))
      first
      :commit/author-date
      dates.tick/parse-time-string))

(defn reset-last-modified [item new-lm]
  (log/info "Setting last-modified for" (:org/short-path item) new-lm)
  (fs/set-last-modified-time
    (:org/source-file item) (t/instant new-lm)))

(defn reset-last-modified-via-git [item]
  (let [commit-dt (-> item :org/source-file last-commit-dt)]
    (when-not commit-dt
      (log/warn "No latest commit for item" (:org/short-path item)))
    (when commit-dt
      (if (and (:file/last-modified item)
               (t/>= commit-dt (:file/last-modified item)))
        (log/debug "Latest commit dt is AFTER last-modified, skipping lm reset"
                   (:org/short-path item))
        (reset-last-modified item commit-dt)))))

(defn reset-created-at-via-git [item]
  (when (#{:level/root} (:org/level item))
    (let [first-commit-dt (-> item :org/source-file first-commit-dt)
          created-at      (-> item :org.props/created-at)]
      (when first-commit-dt
        (if (and created-at (t/>= first-commit-dt created-at))
          (log/debug "First commit dt is AFTER item created-at, skipping created-at reset"
                     (:org/short-path item))
          (org-crud.update/update! item {:org.props/created-at first-commit-dt}))))))

(comment
  (-> "~/todo/garden/dino.org" last-commit-dt)
  (-> "~/todo/daily/2020-07-15.org" last-commit-dt)
  (-> "~/todo/daily/2020-07-15.org" path->nested-item)
  (-> "~/todo/daily/2023-04-25.org" path->nested-item)
  (-> "~/todo/daily/2020-12-26.org" path->nested-item)

  (-> "~/todo/daily/2023-04-25.org" path->nested-item)
  (-> "~/todo/daily/2023-04-25.org" first-commit-dt)
  (-> "~/todo/daily/2023-04-25.org" path->nested-item reset-created-at-via-git)

  (->>
    (all-garden-notes-nested)
    (map path->nested-item)
    (remove nil?)
    (remove empty?)
    (sort-by :file/last-modified dates.tick/sort-latest-first)
    (map reset-last-modified-via-git)
    doall))
