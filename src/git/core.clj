(ns git.core
  (:require
   [ralphie.git :as r.git]
   [db.core :as db]
   [babashka.fs :as fs]
   [clawe.wm :as wm]
   [clojure.string :as string]
   [taoensso.timbre :as log]))

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
     :repo/directory  (str (fs/home) "/" user-name "/" repo-name)
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
  (clawe-git-dirs))

(defn ingest-clawe-repos []
  (let [dirs  (clawe-git-dirs)
        repos (->> dirs (map dir->db-repo))]
    (log/info "Ingesting" (count repos) "repos from clawe")
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
  (fetch-repo {:repo/directory  (str (fs/home) "/russmatney/clawe")
               :repo/short-path "russmatney/clawe"})
  ;; ambiguous still works...
  (fetch-repo {:repo/directory  (str (fs/home) "~/teknql/fabb")
               :repo/short-path "russmatney/clawe"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->db-commit [commit]
  (cond-> commit
    true
    (assoc :doctor/type :type/commit)

    (:commit/directory commit)
    (->
      (assoc :commit/directory (str (:commit/directory commit)))
      (assoc :commit/repo [:repo/directory (str (:commit/directory commit))]))

    true
    ;; TODO support these in the db!
    (dissoc :commit/stat-lines)))

(defn commits [opts]
  (let [commits
        (try
          (r.git/commits opts)
          (catch Exception _e nil))
        commit-stats
        (try
          (r.git/commit-stats opts)
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
        (map #(commits {:dir % :n n}))
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
        (log/info "syncing" (count commits) "commits to the db")
        (db/transact commits))
      (log/warn "No commits found for opts" opts))))

;; TODO malli type hints for inputs like these!
(defn ingest-commits-for-repo
  ([repo] (ingest-commits-for-repo repo nil))
  ([repo opts]
   (let [n (:n opts 100)]
     (if-let [db-repo (fetch-repo repo)]
       (do
         (log/info "ingesting commits for repo" db-repo)
         (sync-commits-to-db
           {:dirs [(:repo/directory db-repo)]
            :n    n}))
       (log/warn "No DB Repo for repo desc" repo)))))

(comment
  (fetch-repo {:repo/short-path "russmatney/clawe"})
  (ingest-commits-for-repo {:repo/short-path "russmatney/clawe"})
  (ingest-commits-for-repo {:repo/short-path "russmatney/dino"})
  (ingest-commits-for-repo {:repo/short-path "russmatney/dotfiles"}))

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
    (list-db-commits))

  (db/query
    '[:find (pull ?c [*]) (pull ?r [*])
      :where
      [?c :doctor/type :type/commit]
      [?c :commit/repo ?r]
      [?r :doctor/type :type/repo]]))
