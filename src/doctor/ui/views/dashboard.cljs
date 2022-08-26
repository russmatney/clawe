(ns doctor.ui.views.dashboard
  (:require
   [hooks.topbar :as hooks.topbar]
   [hooks.workspaces :as hooks.workspaces]

   [doctor.ui.hooks.use-topbar :as use-topbar]
   [doctor.ui.views.topbar :as topbar]
   [doctor.ui.db :as ui.db]
   [components.todo :as components.todo]
   [components.events :as components.events]
   [pages.db :as pages.db]
   [components.filter :as components.filter]
   [pages.todos :as pages.todos]))

(defn widget [opts]
  (let [metadata                                      (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as metadata} @metadata
        {:keys [active-workspaces]}                   (hooks.workspaces/use-workspaces)
        topbar-state                                  (use-topbar/use-topbar-state)]
    [:div
     {:class ["text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}

     [:div
      {:class ["grid" "grid-flow-col"]}

      ;; workspaces
      [topbar/workspace-list topbar-state active-workspaces]

      ;; current task
      [topbar/current-task opts]

      ;; clock/host/metadata
      [topbar/clock-host-metadata topbar-state metadata]]

     [pages.db/ingest-buttons]

     (let [todos         (ui.db/queued-todos (:conn opts))
           recent-events (ui.db/events (:conn opts))

           {:keys [filtered-item-groups filtered-items filter-grouper]}
           (components.filter/use-filter
             {:all-filter-defs  pages.todos/all-filter-defs
              :default-filters  pages.todos/default-filters
              :default-group-by pages.todos/default-group-by
              :items            recent-events})
           recent-events filtered-items
           ]
       [:div
        [:div "Todo list"]
        [components.todo/todo-list nil todos]
        [:div "Events Cluster"]
        filter-grouper
        #_[components.events/event-cluster nil todos]
        ;; show filtered-item-groups?
        [components.events/event-clusters nil recent-events]])]))
