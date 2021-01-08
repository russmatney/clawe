(ns clawe.workspaces
  (:require
   [ralph.defcom :refer [defcom]]
   [clawe.awesome :as awm]))

(defn update-workspaces
  ([] (update-workspaces "update_workspaces_widget"))
  ([fname]
   (let [workspaces [
                     {:name "wkspc"}
                     {:name "wkspc2"}
                     {:name "wkspc3"}
                     ]]
     (awm/awm-cli
       (awm/awm-fn fname workspaces)))))

(comment
  (update-workspaces))


(defcom update-workspaces-cmd
  {:name "update-workspaces"

   :one-line-desc
   "updates the workspaces widget to reflect the current workspaces state."
   :description
   ["expects a function-name as an argument,
which is called with a list of workspaces maps."]
   :handler (fn [_ parsed]
              (update-workspaces (-> parsed :arguments first)))})

nil
