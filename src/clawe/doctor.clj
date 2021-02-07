(ns clawe.doctor
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.notify :as notify]))

(defcom doctor-cmd
  {:defcom/name    "clawe-doctor"
   :defcom/handler
   (fn [_ _]
     (println "doctor-cmd called")
     (notify/notify "doctor-cmd called")

     )})

(comment
  ;; TODO fix this arity problem in defcom, it's quite annoying
  (doctor-cmd nil nil))
