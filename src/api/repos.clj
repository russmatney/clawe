(ns api.repos
  (:require
   [babashka.fs :as fs]
   [taoensso.telemere :as log]
   [tick.core :as t]
   [clojure.string :as string]

   [clawe.wm :as wm]
   [dates.tick :as dt]
   [db.core :as db]
   [ralphie.git :as r.git]))

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
    (log/log! :info ["Ingesting" (count repos) "repos from clawe"])
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
        (log/log! :info ["syncing" (count commits) "commits to the db"])
        (db/transact commits))
      (log/log! :info ["No commits found for opts" opts]))))

;; TODO malli type hints for inputs like these!
(defn ingest-commits-for-repo
  ([repo] (ingest-commits-for-repo repo nil))
  ([repo opts]
   (let [n (:n opts 100)]
     (if-let [db-repo (fetch-repo repo)]
       (do
         (log/log! :info ["ingesting commits for repo" db-repo])
         (sync-commits-to-db
           {:dirs [(:repo/directory db-repo)]
            :n    n}))
       (log/log! :warn ["No DB Repo for repo desc" repo])))))

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


(defn update-repo-status [repo]
  (log/log! {:data (:repo/directory repo)} "fetching and checking repo status")
  ;; this likely won't finish before the next status check runs
  ;; maybe we want to wait a tick?
  (r.git/fetch-via-tmux (:repo/directory repo))
  (future
    ;; wait a tick for the above tmux/fire to finish
    (Thread/sleep 2000)
    (let [{:git/keys [dirty? needs-pull? needs-push? last-fetch-timestamp]}
          (r.git/status (:repo/directory repo))
          now    (dt/now)
          update (cond-> {:repo/last-fetch-timestamp last-fetch-timestamp}
                   ;; TODO hammock git status attributes
                   dirty?            (assoc :repo/dirty-at now)
                   (not dirty?)      (assoc :repo/clean-at now)
                   needs-pull?       (assoc :repo/needs-pull-at now)
                   (not needs-pull?) (assoc :repo/did-not-need-pull-at now)
                   needs-push?       (assoc :repo/needs-push-at now)
                   (not needs-push?) (assoc :repo/did-not-need-push-at now))]
      (log/log! {:data (:repo/directory repo)} ["updated repo git status"])
      (db/transact (merge repo update) {:verbose? true})))
  :ok)


(comment
  (update-repo-status {:repo/directory "~/russmatney/glossolalia"})
  )

(defn tracked-repo?
  ;; TODO support a db-field that we set from the frontend/cli
  ;; something like :repo/track-git-status
  [repo]
  (and
    (#{"russmatney"} (:repo/user-name repo))
    (#{"clawe"
       "dotfiles"
       "word-games"
       "dino"
       "bones"} (:repo/name repo))))


(defn get-repos
  ([] (get-repos nil))
  ([_opts]
   (->> (db/query
          '[:find (pull ?e [*])
            :where
            [?e :doctor/type :type/repo]])
        (map first))))

(comment
  (->>
    (get-repos)
    (map keys)
    (into #{})))

(defn refresh-git-status
  "For opted-in repos, check and update the dirty/needs-pull/needs-push status.
  "
  []
  (->>
    (get-repos)
    (filter tracked-repo?)
    ;; TODO i'm sure we want some caching strategy here, but i'm not sure what it is
    ;; maybe once per pomodoro, so we rely on it for a fresh context?
    (map update-repo-status)
    doall))

(comment
  (refresh-git-status)
  )



(defn get-commits
  ([] (get-commits nil))
  ([opts]
   (let [after  (:after opts (-> (t/today)
                                 (t/at (t/midnight))
                                 dt/add-tz))
         before (:before opts (dt/now))

         commits
         (->> (db/query
                '[:find (pull ?e [*])
                  :in $ ?after ?before
                  :where
                  [?e :doctor/type :type/commit]
                  [?e :event/timestamp ?timestamp]
                  [(t/> ?timestamp ?after)]
                  [(t/< ?timestamp ?before)]]
                after before)
              (map first)
              (sort-by :event/timestamp dt/sort-latest-first))]
     commits)))

(comment
  (get-commits)
  (get-commits
    {:after  (-> (t/yesterday) (t/at (t/midnight)) dt/add-tz)
     :before (dt/now)})

  (list-db-commits))
