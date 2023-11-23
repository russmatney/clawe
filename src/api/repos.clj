(ns api.repos
  (:require
   [taoensso.timbre :as log]
   [tick.core :as t]
   [ralphie.git :as ralphie.git]
   [dates.tick :as dt]
   [db.core :as db]
   [git.core :as git]))

(defn check-repo-status [repo]
  (log/info "checking repo status" repo)
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
    (log/info "update" update)
    (db/transact (merge repo update) {:verbose? true}))
  :ok)

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
