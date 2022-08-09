(ns git.core
  (:require
   [ralphie.git :as r.git]
   [db.core :as db]
   [babashka.fs :as fs]
   [clawe.wm :as wm]
   [ralphie.zsh :as zsh]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this doesn't work for ~/todo (ends up with /home/russ/russ/todo)
;; TODO add a few unit tests
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

(defn db-repos
  "Fetches git-dirs for all :workspace/directories in the db"
  []
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :doctor/type :type/repo]])
    (map first)))

(comment
  (ingest-clawe-repos)
  (db-repos))

(defn fetch-repo
  "Fetches git-dirs for all :workspace/directories in the db"
  [{:keys [repo/short-path repo/directory]}]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?short-path ?directory
        :where
        [?e :doctor/type :type/repo]
        (or
          [?e :repo/short-path ?short-path]
          [?e :repo/directory ?directory])]
      short-path directory)
    ffirst))

(comment
  (fetch-repo {:repo/short-path "russmatney/clawe"})
  (fetch-repo {:repo/short-path "notreal"})
  (fetch-repo {:repo/directory  (zsh/expand "~/russmatney/clawe")
               :repo/short-path "russmatney/clawe"})
  ;; ambiguous still works...
  (fetch-repo {:repo/directory  (zsh/expand "~/teknql/fabb")
               :repo/short-path "russmatney/clawe"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->db-commit [commit]
  (cond-> commit
    true
    (assoc :doctor/type :type/commit)

    ;; TODO proper ref between these
    (:commit/directory commit)
    (assoc :commit/repo {:repo/directory (:commit/directory commit)})))

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
  [{:keys [n dirs]}]
  (let [n (or n 10)]
    (when dirs
      (->>
        dirs
        (map #(commits-for-dir {:dir % :n n}))
        (remove nil?)
        (apply concat)))))

(comment
  (count
    (commits-for-git-dirs {:n 10})))

(defn sync-commits-to-db
  [opts]
  (let [commits (->> (commits-for-git-dirs opts)
                     (remove nil?)
                     (map ->db-commit))]
    (if (seq commits)
      (do
        (println "syncing" (count commits) "commits to the db")
        (def commits commits)
        (db/transact commits))
      (println "No commits found for opts" opts))))

(defn ingest-commits-for-repo [repo]
  (if-let [db-repo (fetch-repo repo)]
    (do
      (println "ingesting commits for repo" db-repo)
      (sync-commits-to-db
        {:dirs [(:repo/directory db-repo)]
         :n    30}))
    (println "No DB Repo for repo desc" repo)))

(comment
  (ingest-commits-for-repo {:repo/short-path "russmatney/clawe"})
  )

(defn list-db-commits []
  (->>
    ;; TODO get timestamps into db and sort by most recent in this fn
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :doctor/type :type/commit]])
    (map first)))

(comment
  (count
    (list-db-commits)))
