(ns git.core
  (:require
   [ralphie.git :as r.git]
   [defthing.db :as db]
   [babashka.fs :as fs]
   [clawe.wm :as wm]
   [ralphie.zsh :as zsh]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dir->db-repo [dir]
  (let [reversed  (-> dir (string/split #"/") reverse)
        repo-name (first reversed)
        user-name (second reversed)]
    {:repo/name       repo-name
     :repo/user-name  user-name
     :repo/short-path (str user-name "/" repo-name)
     :repo/directory  (zsh/expand "~/" user-name "/" repo-name)
     :doctor/type     :type/repo}))

(defn is-git-dir? [dir]
  (when dir
    (fs/exists? (str dir "/.git"))))

(defn clawe-git-dirs []
  (->>
    (wm/workspace-defs)
    (map :workspace/directory)
    (filter is-git-dir?)))

(comment
  (wm/workspace-defs)
  (clawe-git-dirs)
  )

(defn ingest-clawe-repos []
  (let [dirs  (clawe-git-dirs)
        repos (->> dirs (map dir->db-repo))]
    (println "Ingesting" (count repos) "repos from clawe")
    (db/transact repos)))

(comment
  (ingest-clawe-repos))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
    ;; these should have the same :commit/hash, so will merge in the db
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
        [?e :commit/hash ?hash]])
    (map first)))

(comment
  (count
    (list-db-commits)))
