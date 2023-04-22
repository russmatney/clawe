(ns doctor.ui.views.dashboard
  (:require
   [doctor.ui.db :as ui.db]
   [components.todo :as components.todo]
   [components.events :as components.events]
   [doctor.ui.views.ingest :as ingest]
   [components.filter :as components.filter]
   [uix.core.alpha :as uix]
   [components.actions :as components.actions]
   [components.filter-defs :as filter-defs]
   [components.icons :as components.icons]

   [doctor.ui.pomodoros :as pomodoros]
   [doctor.ui.views.focus :as focus]
   [doctor.ui.views.todos :as todos]
   [doctor.ui.views.blog :as blog]
   [doctor.ui.views.pomodoro :as pomodoro]
   [doctor.ui.views.workspaces :as workspaces]
   [hooks.workspaces :as hooks.workspaces]
   [hiccup-icons.octicons :as octicons]))

(defn widget-bar [{:keys [comp label opts initial-show actions icon]}]
  (let [show?     (uix/state initial-show)
        icon-opts {:class ["px-1 py-2 pr-2"]
                   :text  label}]
    [:div {:class ["flex flex-col"]}
     [:div
      {:class ["flex flex-row"
               "items-center"
               "bg-city-aqua-light-900"
               "border-city-aqua-light-500"
               "border-t"
               "sticky" "top-0"
               "py-1" "px-2"
               "z-5"]}

      [components.icons/icon-comp
       (cond icon  (assoc icon-opts :icon icon)
             :else (assoc icon-opts :icon octicons/alert))]

      [:span
       {:class ["font-mono" "pl-2"]}
       (str ":doctor/" label)]
      [:div
       {:class ["ml-auto"]}
       [components.actions/actions-list
        {:n 5
         :actions
         (concat [{:action/on-click (fn [_] (swap! show? not))
                   :action/label    "Show"
                   :action/icon     octicons/chevron-down16
                   :action/disabled @show?}
                  {:action/on-click (fn [_] (swap! show? not))
                   :action/label    "Hide"
                   :action/icon     octicons/chevron-up16
                   :action/disabled (not @show?)}]
                 actions)}]]]
     (when @show? [comp (or opts {})])]))

(defn widget [opts]
  [:div
   {:class ["text-city-pink-200"
            "text-bg-yo-blue-800"]}

   [:div
    {:class ["relative"]}

    [widget-bar {:comp    (fn [_opts]
                            [:div
                             [ingest/ingest-buttons]
                             [ingest/commit-ingest-buttons (:conn opts)]])
                 :label   "ingestors"
                 :actions (ingest/ingest-actions)}]

    [widget-bar {:comp    pomodoro/widget
                 :label   "pomodoro"
                 :opts    opts
                 :icon    octicons/clock16
                 :actions (pomodoros/actions)}]

    [widget-bar {:comp  focus/widget
                 :label "current-focus"
                 :opts  opts
                 :icon  octicons/light-bulb16
                 :actions
                 [{:action/label    "Clear Current Todos"
                   :action/on-click #(js/alert "todo")}]}]

    [widget-bar {:comp  todos/widget
                 :label "todos"
                 :icon  octicons/checklist16
                 :opts  opts
                 :actions
                 [{:action/label    "Process Prioritized Actions"
                   :action/on-click #(js/alert "todo")}]}]

    [widget-bar {:comp  blog/widget
                 :label "blog"
                 :icon  blog/icon
                 :actions
                 [{:action/label    "Publish N Updated notes"
                   :action/on-click #(js/alert "todo")}]}]

    [widget-bar {:comp    workspaces/widget
                 :label   "workspaces"
                 :icon    octicons/clippy16
                 :actions (hooks.workspaces/actions)}]

    [widget-bar {:label        "events"
                 :icon         octicons/calendar16
                 :opts         opts
                 :initial-show true
                 :comp
                 (fn [opts]
                   (let [recent-events (ui.db/events (:conn opts))
                         filter-data
                         (components.filter/use-filter
                           (assoc filter-defs/fg-config
                                  :items recent-events
                                  :label (str (count recent-events) " Events")))]
                     [:div
                      {:class ["p-4"]}
                      [:div
                       (:filter-grouper filter-data)

                       [components.filter/items-by-group
                        (assoc filter-data :group->comp components.events/event-clusters)]]]))
                 :actions [{:action/label "Today's events"}
                           {:action/label "This week's events"}
                           {:action/label "Today's screenshots"}]}]

    [widget-bar
     {:label "old-todos"
      :icon  octicons/checklist16
      :opts  opts
      :comp
      (fn [opts]
        (let [current-todos (ui.db/current-todos (:conn opts))
              all-todos     (ui.db/list-todos
                              (:conn opts) {:n 200 ;; TODO support fetching more
                                            :filter-pred
                                            (fn [{:org/keys [status]}]
                                              (#{:status/not-started} status))})
              todo-filter-results
              (components.filter/use-filter
                (assoc filter-defs/fg-config :items all-todos))]
          [:div
           [:div
            [:div "Todo list (" (count current-todos) ")"]
            [components.todo/todo-list {:n 5} current-todos]]

           (let [expanded (uix/state
                            ;; default to hiding all todos if some are queued
                            (< (count current-todos) 2))]
             [:div
              [:div "All todos (" (count (:filtered-items todo-filter-results)) ")"
               (count all-todos)]
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
                 [components.todo/todo-list
                  {:n 5}
                  (:filtered-items todo-filter-results)]])])]))}]]])
