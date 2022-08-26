(ns doctor.ui.views.dashboard
  (:require
   [hooks.topbar :as hooks.topbar]
   [hooks.workspaces :as hooks.workspaces]

   [doctor.ui.hooks.use-topbar :as use-topbar]
   [doctor.ui.views.topbar :as topbar]
   [doctor.ui.db :as ui.db]
   [components.todo :as components.todo]
   [components.events :as components.events]))

(defn widget [opts]
  (let [metadata                                      (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as metadata} @metadata
        {:keys [active-workspaces]}                   (hooks.workspaces/use-workspaces)
        topbar-state                                  (use-topbar/use-topbar-state)]
    [:div
     {:class ["h-screen" "overflow-hidden" "text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}
     [:div
      {:class ["grid" "grid-flow-row"]}

      ;; workspaces
      [topbar/workspace-list topbar-state active-workspaces]

      ;; current task
      [topbar/current-task opts]

      ;; clock/host/metadata
      [topbar/clock-host-metadata topbar-state metadata]]


     (let [todos         (ui.db/queued-todos (:conn opts))
           recent-events (ui.db/events (:conn opts))]
       [:div
        [:div "Todo list"]
        [components.todo/todo-list nil todos]
        [:div "Events Cluster"]
        [components.events/event-cluster nil todos]
        [components.events/event-cluster nil recent-events]])]))
