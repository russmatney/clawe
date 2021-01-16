(ns clawe.workspaces
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.workspace :as r.workspace]
   [clawe.awesome :as awm]
   [ralphie.item :as item]))

(defn active-workspaces
  "Pulls workspaces to show in the workspaces-widget."
  []
  (->>
    (r.workspace/all-workspaces)
    (filter :awesome/tag)
    (map (fn [spc]
           {;; consider flags for is-scratchpad/is-app/is-repo
            :name          (item/awesome-name spc)
            :awesome_index (item/awesome-index spc)
            :key           (-> spc :org.prop/key)
            :fa_icon_code  (when-let [code (:org.prop/fa-icon-code spc)]
                             (str "\\u{" code "}"))
            :scratchpad    (item/scratchpad? spc)
            :selected      (item/awesome-selected spc)
            :empty         (item/awesome-empty spc)
            }))
    (map (fn [spc]
           (assoc spc
                  :sort-key (str (:scratchpad spc) "-" (:awesome_index spc)))))
    (sort-by :sort-key)))

(comment
  (->>
    (r.workspace/all-workspaces)
    (filter :awesome/tag))

  (active-workspaces))

(defn update-workspaces
  ([] (update-workspaces nil))
  ([fname]
   (let [fname (or fname "update_workspaces_widget")]
     (awm/awm-cli
       (awm/awm-fn fname (active-workspaces))))))

(comment
  (->>
    (active-workspaces)
    (map :awesome-index))
  (awm/awm-fn "update_workspaces_widget" (active-workspaces))

  (update-workspaces))

(defcom update-workspaces-cmd
  {:name    "update-workspaces"
   :one-line-desc
   "updates the workspaces widget to reflect the current workspaces state."
   :description
   ["expects a function-name as an argument,
which is called with a list of workspaces maps."]
   :handler (fn [_ parsed]
              (update-workspaces (-> parsed :arguments first)))})

nil
