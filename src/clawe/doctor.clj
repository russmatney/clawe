(ns clawe.doctor
  (:require
   [clawe.workspaces :as workspaces]
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]))

(defcom clawe-doctor
  (do
    (println "doctor-cmd called")
    (notify/notify "doctor-cmd called")
    (let [wsps (->> (workspaces/all-workspaces)
                    (filter :git/check-status?)
                    (map workspaces/apply-git-status)
                    (filter #(or
                               (:git/dirty? %)
                               (:git/needs-pull? %)
                               (:git/needs-push? %))))]
      (doall
        (map
          #(notify/notify
             ":git/status alert"
             (workspaces/wsp->repo-and-status-label %))
          wsps)))))
