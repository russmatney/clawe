(ns clawe.rules
  (:require
   [clawe.workspaces :as workspaces]
   [ralph.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [clojure.edn :as edn]))


(defn enforce-awesome-rules
  "Reads awesome rules from the workspace, updates awesomeWM's global rules obj."
  []
  (let [wsp-awm-rules (->> (workspaces/all-workspaces)
                           (keep :awesome/rules))]
    ;; TODO some awm-fnl function for updating the rules
    wsp-awm-rules))



;; TODO put in doc string
  "Awesome fires this from awesome/rules.fnl for every 'rule event'
(for all new clients, maybe more: see clawe/awesome/rules.fnl)."
(defcom window-callback
  {:defcom/name "window-callback"
   :defcom/handler
   (fn [_ {:keys [arguments]}]
     (let [arg (some-> arguments first edn/read-string)]
       (println "window-callback called" arg)
       (notify/notify "window-callback called" arg)))})
