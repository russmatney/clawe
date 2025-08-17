(ns pages.journal
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.garden :as components.garden]
   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.hooks.use-workspaces :as hooks.use-workspaces]
   [doctor.ui.hooks.plasma :refer [with-rpc]]
   [doctor.ui.handlers :as handlers]

   [doctor.ui.views.workspaces :as views.workspaces]
   ))

(defn use-recent-garden-notes []
  (let [[notes set-notes] (uix/use-state nil)
        {:keys [data]}    (hooks.use-db/use-query
                            {:db->data #(->> (ui.db/garden-files %) (take 3))})
        recent-file-paths data]
    (with-rpc [data]
      (when recent-file-paths
        (->> recent-file-paths
             (map (fn [p] {:org/source-file p}))
             handlers/full-garden-items))
      set-notes)
    {:notes notes}))

(defui page [opts]
  (let [{:keys [notes]} (use-recent-garden-notes)

        {:keys [active-workspaces
                all-clients]}
        (hooks.use-workspaces/use-workspaces)

        journal-workspace (some->> active-workspaces
                                   (filter #(#{"journal"} (:workspace/title %)))
                                   first)
        ]
    ($ :div
       (for [[i note] (map-indexed vector notes)]
         ($ :div
            {:key   i
             :class ["p-2"
                     "bg-slate-800"
                     "xl:mx-48" "mx-16" "mt-16"]}

            ($ components.garden/org-file (assoc opts :item note))))

       ;; TODO add a workspace comp
       ($ views.workspaces/workspace-card (assoc opts :workspace journal-workspace))


       ;; TODO add an active-app lists


       )))
