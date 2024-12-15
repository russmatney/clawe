(ns doctor.ui.views.dashboard
  (:require
   ["react-icons/fa6" :as FA]
   ["react-icons/md" :as MD]
   ["react-icons/gi" :as GI]
   ["react-icons/go" :as GO]
   [uix.core :as uix :refer [$ defui]]

   [doctor.ui.db :as ui.db]
   [doctor.ui.views.ingest :as ingest]

   [components.events :as components.events]
   [components.filter :as components.filter]
   [components.actions :as components.actions]
   [components.filter-defs :as filter-defs]
   [components.icons :as components.icons]

   ;; TODO port to doctor.ui.view
   [pages.screenshots :as screenshots]
   [doctor.ui.views.focus :as focus]
   [doctor.ui.views.todos :as todos]
   [doctor.ui.views.commits :as commits]
   [doctor.ui.views.today :as today]
   [doctor.ui.views.blog :as blog]
   [doctor.ui.views.pomodoro :as pomodoro]
   [doctor.ui.views.git-status :as git-status]
   [doctor.ui.views.chess-games :as chess-games]
   [doctor.ui.views.workspaces :as workspaces]
   [doctor.ui.hooks.use-workspaces :as hooks.use-workspaces]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.localstorage :as localstorage]))

(defui widget-bar
  [{:keys [comp label opts actions icon w-class bar]}]
  (let [icon-opts        {:class ["px-1 py-2 pr-2"]
                          :text  label}
        storage-label    (str  "widget-bar-" label)
        cached-show      (localstorage/get-item storage-label)
        [show? set-show] (uix/use-state cached-show)]
    ($ :div
       {:class ["flex flex-col" (or w-class "w-full")]}
       ($ :div
          {:class ["flex flex-row"
                   "items-center"
                   "bg-city-aqua-light-900"
                   "border-city-aqua-light-500"
                   "border-t"
                   "sticky" "top-0"
                   "py-1" "px-2" "z-10"]}

          ($ components.icons/icon-comp
             (cond icon  (assoc icon-opts :icon icon)
                   :else (assoc icon-opts :icon FA/FaTriangleExclamation)))

          ($ :span
             {:class ["font-mono" "pl-2"]}
             (str ":doctor/" label))

          (when bar
            ($ :span {:class ["ml-auto"]}
               ($ bar opts)))

          ($ :div
             {:class ["ml-auto"]}
             ($ components.actions/actions-list
                {:n 2
                 :actions
                 (concat [{:action/on-click (fn [_]
                                              (set-show true)
                                              (localstorage/set-item! storage-label true))
                           :action/label    "Show"
                           :action/icon     FA/FaChevronDown
                           :action/disabled show?}
                          {:action/on-click (fn [_]
                                              (set-show false)
                                              (localstorage/remove-item! storage-label))
                           :action/label    "Hide"
                           :action/icon     FA/FaChevronUp
                           :action/disabled (not show?)}]
                         actions)})))
       (when show? ($ comp (or opts {}))))))


(defui widget [opts]
  ($ :div
     {:class ["text-city-pink-200"
              "text-bg-yo-blue-800"]}

     ($ :div
        {:class ["relative"]}

        ($ :div
           {:class ["flex" "flex-row" "flex-wrap"]}

           ($ widget-bar {:comp    (fn [_opts]
                                     ($ :div
                                        ($ ingest/ingest-buttons)
                                        ($ ingest/commit-ingest-buttons opts)))
                          :label   "ingestors"
                          :icon    FA/FaDatabase
                          :opts    opts
                          :actions (ingest/ingest-actions)})

           ($ widget-bar {:comp  git-status/widget
                          :label "git-status"
                          :icon  FA/FaGithub
                          :opts  opts
                          :bar   git-status/bar})

           ($ widget-bar {:comp  pomodoro/widget
                          :label "pomodoro"
                          :opts  opts
                          :icon  GI/GiTomato
                          :bar   pomodoro/bar})

           ($ widget-bar {:comp  focus/widget
                          :label "current-focus"
                          :opts  opts
                          :icon  FA/FaCheck
                          :actions
                          [{:action/label    "Clear Current Todos"
                            :action/on-click #(handlers/clear-current-todos)}]})

           ($ widget-bar {:comp  todos/widget
                          :label "todos"
                          :icon  FA/FaCheckDouble
                          :opts  (assoc opts :filter-id :dashboard-todos)
                          :actions
                          [{:action/label    "Process Prioritized Actions"
                            :action/on-click #(js/alert "todo")}]})

           ($ widget-bar {:comp  blog/widget
                          :label "blog"
                          :opts  (assoc opts :filter-id :dashboard-blog)
                          :icon  FA/FaNewspaper
                          :actions
                          [{:action/label    "Publish N Updated notes"
                            :action/on-click #(js/alert "todo")}]})

           ($ widget-bar {:comp  today/widget
                          :label "today"
                          :icon  FA/FaCalendar
                          :opts  (assoc opts :filter-id :dashboard-today)})

           ($ widget-bar {:comp  commits/widget
                          :label "commits"
                          :icon  GO/GoGitCommit
                          :opts  opts})

           ($ widget-bar {:comp  screenshots/page
                          :label "screenshots-clips"
                          :opts  opts
                          :icon  FA/FaDesktop
                          :actions
                          [{:action/label    "Ingest screenshots"
                            :action/on-click #(handlers/ingest-screenshots)}
                           {:action/label    "Ingest clips"
                            :action/on-click (fn [_] (handlers/ingest-clips))}]})

           ($ widget-bar {:comp    workspaces/widget
                          :label   "workspaces"
                          :icon    MD/MdDashboard
                          :actions (hooks.use-workspaces/actions)})

           ($ widget-bar {:comp    chess-games/widget
                          :label   "chess games"
                          :opts    opts
                          :icon    FA/FaChessKnight
                          :actions (chess-games/actions)})

           ($ widget-bar {:label "events"
                          :icon  FA/FaCalendar
                          :opts  opts
                          :comp
                          (fn [opts]
                            (let [recent-events (ui.db/events (:conn opts))
                                  filter-data
                                  (components.filter/use-filter
                                    (assoc filter-defs/fg-config
                                           :id :dashboard-events
                                           :items recent-events
                                           :label (str (count recent-events) " Events")))]
                              ($ :div
                                 {:class ["p-4"]}
                                 ($ :div
                                    (:filter-grouper filter-data)

                                    ($ components.filter/items-by-group
                                       (assoc filter-data :group->comp #($ components.events/event-clusters {:events (:group %)})))))))
                          :actions [{:action/label "Today's events"}
                                    {:action/label "This week's events"}
                                    {:action/label "Today's screenshots"}]})))))
