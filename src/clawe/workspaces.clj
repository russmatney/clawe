(ns clawe.workspaces
  (:require
   [ralph.defcom :refer [defcom]]
   [clawe.awesome :as awm]))

(defn update-workspaces
  ([] (update-workspaces "update_workspaces_widget"))
  ([fname]
   (let [workspaces [{:name "wkspc"}]]
     (awm/awm-cli
       (awm/awm-fn fname workspaces)))))

(comment
  (update-workspaces))


(defcom update-workspaces-cmd
  {:name    "update-workspaces"
   :one-line-desc
   "Updates the workspaces widget to reflect the current workspaces state."
   :description
   ["Expects a function-name as an argument,
which is called with a list of workspaces maps."]
   :handler (fn [_ parsed]
              (update-workspaces (-> parsed :arguments first)))})

nil
