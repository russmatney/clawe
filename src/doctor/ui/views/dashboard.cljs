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
   [pages.todos :as pages.todos]
   [uix.core.alpha :as uix]
   [components.actions :as components.actions]))

(defn widget [opts]
  (let [metadata                                       (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as _metadata} @metadata
        {:keys [_active-workspaces]}                   (hooks.workspaces/use-workspaces)
        _topbar-state                                  (use-topbar/use-topbar-state)]
    [:div
     {:class ["text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}

     [:div
      {:class ["grid" "grid-flow-col"]}

      ;; workspaces
      #_[topbar/workspace-list topbar-state active-workspaces]

      ;; current task
      [topbar/current-task opts]

      ;; clock/host/metadata
      #_[topbar/clock-host-metadata topbar-state metadata]]

     [pages.db/ingest-buttons]

     (let [queued-todos  (ui.db/queued-todos (:conn opts))
           recent-events (ui.db/events (:conn opts))

           event-filter-results
           (components.filter/use-filter
             {:all-filter-defs  pages.todos/all-filter-defs
              :default-filters  pages.todos/default-filters
              :default-group-by pages.todos/default-group-by
              :items            recent-events})
           all-todos (ui.db/garden-todos (:conn opts))
           todo-filter-results
           (components.filter/use-filter
             {:all-filter-defs  pages.todos/all-filter-defs
              :default-filters  pages.todos/default-filters
              :default-group-by pages.todos/default-group-by
              :items            all-todos})]

       [:div
        [:div
         [:div "Todo list"]
         [components.todo/todo-list nil queued-todos]]

        (let [expanded (uix/state
                         ;; default to hiding all todos if some are queued
                         (< (count queued-todos) 2))]
          [:div
           [:div "All todos"]
           [components.actions/actions-list
            {:actions [{:action/on-click (fn [_] (swap! expanded not))
                        :action/label    "Expand"
                        :action/disabled @expanded}
                       {:action/on-click (fn [_] (swap! expanded not))
                        :action/label    "Collapse"
                        :action/disabled (not @expanded)}]}]
           (when @expanded
             [:div
              (:filter-grouper todo-filter-results)
              [components.todo/todo-list nil (:filtered-items todo-filter-results)]])])

        (let [expanded (uix/state nil)]
          [:div
           [:div "Events Cluster"]
           [components.actions/actions-list
            {:actions [{:action/on-click (fn [_] (swap! expanded not))
                        :action/label    "Expand"
                        :action/disabled @expanded}
                       {:action/on-click (fn [_] (swap! expanded not))
                        :action/label    "Collapse"
                        :action/disabled (not @expanded)}]}]
           (when @expanded
             [:div
              (:filter-grouper event-filter-results)
              ;; show filtered-item-groups?
              [components.events/event-clusters
               nil
               (:filtered-items event-filter-results)]])])])]))
