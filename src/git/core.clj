(ns git.core
  (:require
   [ralphie.git :as r.git]
   [defthing.db :as db]
   [defthing.defworkspace :as defworkspace]

   [babashka.fs :as fs]
   [wing.core :as w]
   [clojure.string :as string]))

(defn is-git-dir? [dir]
  (and
    (fs/exists? (str dir "/.git"))
    (not
      (or
        (string/includes? dir ".emacs.d")
        (string/includes? dir "bb-filewatcher-example")
        (string/includes? dir "some-proj")
        (string/includes? dir "canvas-toying")
        (string/includes? dir "clojuregodottest")
        (string/includes? dir "suit")
        (string/includes? dir "re-dnd")
        (string/includes? dir "find-deps")
        (string/includes? dir "shadow-electron-starter")
        (string/includes? dir "bb-task-completion")))
    (or
      (string/includes? dir "russmatney")
      (string/includes? dir "todo")
      (string/includes? dir "teknql"))))

;; TODO move to git/db ns
(defn db-git-dirs
  "Fetches git-dirs for all :workspace/directories in the db"
  []
  (->>
    (db/query
      '[:find ?directory
        ;; TODO maybe repos/workspaces opt-in to git history?
        :where
        [?e :workspace/directory ?directory]])
    (map first)
    (filter is-git-dir?)))

(defn commits-for-dir [opts]
  (println "commits-for-dir" opts)
  (let [commits
        (try
          (r.git/commits-for-dir opts)
          (catch Exception _e nil))
        commit-stats
        (try
          (r.git/commit-stats-for-dir opts)
          (catch Exception _e nil))]
    ;; these should have the same :git.commit/hash, so will merge in the db
    (->> (concat [] commits commit-stats)
         (remove nil?))))

(defn commits-for-git-dirs
  ([] (commits-for-git-dirs {}))
  ([{:keys [n dirs]}]
   (let [n (or n 10)]
     (->>
       (or dirs (db-git-dirs))
       (map #(commits-for-dir {:dir % :n n}))
       (remove nil?)
       (apply concat)))))

(comment
  (count
    (commits-for-git-dirs {:n 10})))

(defn sync-commits-to-db
  ([] (sync-commits-to-db {}))
  ([opts]
   (doall
     (->> (commits-for-git-dirs opts)
          (map db/transact)))))

(comment
  (sync-commits-to-db {:n 10}))

(defn list-db-commits []
  (->>
    ;; TODO get timestamps into db and sort by most recent in this fn
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :git.commit/hash ?hash]])
    (map first)))

(comment
  (count
    (list-db-commits)))
