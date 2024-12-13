(ns api.repos
  (:require
   [taoensso.telemere :as log]
   [tick.core :as t]

   [dates.tick :as dt]
   [db.core :as db]
   [git.core :as git]
   [ralphie.git :as ralphie.git]))

(defn update-repo-status [repo]
  (log/log! :info ["fetching and checking repo status" repo])
  ;; this likely won't finish before the next status check runs
  ;; maybe we want to wait a tick?
  (ralphie.git/fetch-via-tmux (:repo/directory repo))
  (let [{:git/keys [dirty? needs-pull? needs-push?]}
        (ralphie.git/status (:repo/directory repo))
        now    (dt/now)
        update (cond-> {}
                 ;; TODO hammock git status attributes
                 dirty?            (assoc :repo/dirty-at now)
                 (not dirty?)      (assoc :repo/clean-at now)
                 needs-pull?       (assoc :repo/needs-pull-at now)
                 (not needs-pull?) (assoc :repo/did-not-need-pull-at now)
                 needs-push?       (assoc :repo/needs-push-at now)
                 (not needs-push?) (assoc :repo/did-not-need-push-at now))]
    (log/log! :info ["update" (:repo/directory repo)])
    (db/transact (merge repo update) {:verbose? true}))
  :ok)

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

  (git/list-db-commits))
