(ns garden.core
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [manifold.stream :as s]
   [org-crud.core :as org-crud]
   [systemic.core :refer [defsys] :as sys]
   [ralphie.zsh :as r.zsh]
   [util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-last-modified
  [item]
  ;; TODO include this format in parser
  (def i item)
  (-> item :org/source-file fs/last-modified-time str))

(comment
  (-> i :org/source-file fs/last-modified-time str)

  (-> i :org/source-file fs/file-name))

(defn org->garden-node
  [{:org/keys      [source-file]
    :org.prop/keys [title created-at]
    :as            item}]
  (let [last-modified (get-last-modified item)]
    (->
      item
      (dissoc :org/items)
      (assoc :garden/file-name (fs/file-name source-file)
             :org/short-path (-> source-file
                                 (string/replace-first "/home/russ/todo/" "")
                                 (string/replace-first "/Users/russ/todo/" ""))
             :org.prop/created-at created-at
             :org.prop/title (or title (fs/file-name source-file))
             :time/last-modified last-modified))))

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
    (map org->garden-node)
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

  ;; TODO

  - dailies for the last 3 days (today inclusive)
  "
  []
  (->>
    "~/todo/*"
    r.zsh/expand-many
    (filter (fn [source-file]
              (or
                (string/includes? source-file "journal.org")
                (string/includes? source-file "projects.org"))))
    (map org-crud/path->nested-item)
    (map org->garden-node)
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
