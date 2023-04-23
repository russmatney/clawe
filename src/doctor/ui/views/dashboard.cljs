(ns doctor.ui.views.dashboard
  (:require
   [doctor.ui.db :as ui.db]
   [components.events :as components.events]
   [doctor.ui.views.ingest :as ingest]
   [components.filter :as components.filter]
   [uix.core.alpha :as uix]
   [components.actions :as components.actions]
   [components.filter-defs :as filter-defs]
   [components.icons :as components.icons]

   ;; TODO move to doctor.ui.view
   [pages.screenshots :as screenshots]

   [doctor.ui.pomodoros :as pomodoros]
   [doctor.ui.views.focus :as focus]
   [doctor.ui.views.todos :as todos]
   [doctor.ui.views.blog :as blog]
   [doctor.ui.views.pomodoro :as pomodoro]
   [doctor.ui.views.chess-games :as chess-games]
   [doctor.ui.views.workspaces :as workspaces]
   [hooks.workspaces :as hooks.workspaces]
   [hiccup-icons.octicons :as octicons]
   [doctor.ui.handlers :as handlers]))

(defn widget-bar [{:keys [comp label opts initial-show actions icon
                          w-class]}]
  (let [show?     (uix/state initial-show)
        icon-opts {:class ["px-1 py-2 pr-2"]
                   :text  label}]
    [:div
     {:class ["flex flex-col" (or w-class "w-full")]}
     [:div
      {:class ["flex flex-row"
               "items-center"
               "bg-city-aqua-light-900"
               "border-city-aqua-light-500"
               "border-t"
               "sticky" "top-0"
               "py-1" "px-2"
               "z-10"
               ]}

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

    [:div
     {:class ["flex" "flex-row" "flex-wrap"]}

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
                    :action/on-click #(js/alert "todo")}]
                  }]

     [widget-bar {:comp  todos/widget
                  :label "todos"
                  :icon  octicons/checklist16
                  :opts  opts
                  :actions
                  [{:action/label    "Process Prioritized Actions"
                    :action/on-click #(js/alert "todo")}]
                  }]

     [widget-bar {:comp  blog/widget
                  :label "blog"
                  :opts  opts
                  ;; :initial-show true
                  :icon  blog/icon
                  :actions
                  [{:action/label    "Publish N Updated notes"
                    :action/on-click #(js/alert "todo")}]
                  }]

     [widget-bar {:comp  screenshots/page
                  :label "screenshots"
                  ;; :initial-show true
                  :opts  opts
                  :icon  octicons/image16
                  :actions
                  [{:action/label    "Ingest screenshots"
                    :action/on-click #(handlers/ingest-screenshots)}]
                  }]

     [widget-bar {:comp    workspaces/widget
                  :label   "workspaces"
                  :icon    octicons/clippy16
                  :actions (hooks.workspaces/actions)}]

     [widget-bar {:comp    chess-games/widget
                  :label   "chess games"
                  :opts    opts
                  :icon    octicons/moon16
                  :actions (chess-games/actions)}]

     [widget-bar {:label "events"
                  :icon  octicons/calendar16
                  :opts  opts
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
                            {:action/label "Today's screenshots"}]}]]]])
