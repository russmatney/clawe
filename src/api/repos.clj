(ns api.repos
  (:require
   [taoensso.timbre :as log]
   [dates.tick :as dates]
   [ralphie.git :as ralphie.git]
   [db.core :as db]))

(defn check-repo-status [repo]
  (log/info "checking repo status" repo)
  (let [{:git/keys [dirty? needs-pull? needs-push?]}
        (ralphie.git/status (:repo/directory repo))
        now    (dates/now)
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
