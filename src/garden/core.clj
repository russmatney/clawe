(ns garden.core
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [manifold.stream :as s]
   [org-crud.core :as org-crud]
   [systemic.core :refer [defsys] :as sys]
   [ralphie.zsh :as r.zsh]
   [util]
   [dates.tick :as dates.tick]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org file paths
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;; todos

(defn basic-todo-paths []
  (-> "~/todo/{journal,projects}.org" r.zsh/expand-many))

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

;; general helper

(defn org-file-paths
  "Helper for getting a list of org Files. Ensures they all exist.
  Defaults to some recent dailies, journal.org, projects.org."
  ([] (org-file-paths (concat (basic-todo-paths) (daily-paths 3))))
  ([org-paths] (->> org-paths (map fs/file) (filter fs/exists?))))

(comment
  (org-file-paths)
  (org-file-paths (repo-todo-paths #{"russmatney/clawe" "russmatney/org-crud" "doesnot-exist"}))
  (org-file-paths (daily-paths 14)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org item
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-last-modified
  [item]
  ;; TODO include this format in parser
  (def i item)
  (-> item :org/source-file fs/last-modified-time str))

(comment
  (-> (daily-path) fs/last-modified-time)
  (-> i :org/source-file fs/last-modified-time str)
  (-> i :org/source-file fs/file-name))

(defn org->garden-note
  [{:org/keys      [source-file]
    :org.prop/keys [title created-at]
    :as            item}]
  (let [last-modified (get-last-modified item)]
    (->
      item
      ;; (dissoc :org/items)
      (assoc :garden/file-name (fs/file-name source-file)
             :org/short-path (-> source-file
                                 (string/replace-first "/home/russ/todo/" "")
                                 (string/replace-first "/Users/russ/todo/" ""))
             :org.prop/created-at created-at
             :org.prop/title (or title (fs/file-name source-file))
             :time/last-modified last-modified))))

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

(defn paths->flat-garden-notes [paths]
  (->> paths
       org-file-paths
       (mapcat org-crud/path->flattened-items)
       (map org->garden-note)))

(comment
  (paths->flat-garden-notes (daily-paths)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-dir-files
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO wtf is this?
(defn todo-dir-files []
  (->>
    "~/todo"
    r.zsh/expand
    (org-crud/dir->nested-items {:recursive? true})
    ;; TODO refactor to filter on filename before parsing via org-crud
    (remove (fn [{:org/keys [source-file]}]
              (or
                (string/includes? source-file "/journal/")
                (string/includes? source-file "/urbint/")
                (string/includes? source-file "/archive/")
                (string/includes? source-file "/old/")
                (string/includes? source-file "/old-nov-2020/")
                (string/includes? source-file "/kata/")
                (string/includes? source-file "/standup/")
                (string/includes? source-file "/drafts-journal/")
                ;; (string/includes? source-file "/daily/")
                )))
    (map org->garden-note)
    (sort-by :org/source-file)))

(comment
  (->>
    (todo-dir-files)
    (take 3)))

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
    (todo-dir-files)
    (map util/drop-complex-types))
  )

(comment
  (->>
    (todo-dir-files)
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
  [{:org/keys [source-file]
    :as       item}]
  (def --item item)
  (println "garden.core full-item" item)
  (org-crud/path->nested-item source-file))

(comment
  (full-item --item)
  )
