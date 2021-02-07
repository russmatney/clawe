(ns clawe.doctor
  (:require
   [clawe.workspaces :as workspaces]
   [ralph.defcom :refer [defcom]]
   [ralphie.notify :as notify]))

(defcom doctor-cmd
  {:defcom/name "clawe-doctor"
   :defcom/handler
   (fn [_ _]
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
           wsps))))})

(comment
  ;; TODO fix this arity problem in defcom, it's quite annoying
  (doctor-cmd nil nil))
