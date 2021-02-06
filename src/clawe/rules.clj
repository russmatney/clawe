(ns clawe.rules
  (:require [clawe.workspaces :as workspaces]
            [clawe.awesome :as awm]))


(defn enforce-awesome-rules
  "Reads awesome rules from the workspace, updates awesomeWM's global rules obj."
  []
  (let [wsp-awm-rules (->> (workspaces/all-workspaces)
                           (keep :awesome/rules))]
    ;; TODO some awm-fnl function for updating the rules
    wsp-awm-rules))
