(ns doctor.ui.views.dashboard
  (:require
   [doctor.ui.db :as ui.db]
   [components.todo :as components.todo]
   [components.events :as components.events]
   [doctor.ui.views.ingest :as ingest]
   [components.filter :as components.filter]
   [uix.core.alpha :as uix]
   [components.actions :as components.actions]
   [clojure.string :as string]
   [components.filter-defs :as filter-defs]

   [doctor.ui.views.focus :as focus]
   [doctor.ui.views.blog :as blog]
   [hiccup-icons.octicons :as octicons]))

(def all-todos-initial-filters
  #{{:filter-key :short-path
     :match-fn   #(some-> % (string/split #"/") first #{"daily"})
     :label      "Dailies"}
    {:filter-key :short-path :match "todo/journal.org"}
    {:filter-key :short-path :match "todo/project.org"}
    {:filter-key :status :match :status/not-started}
    ;; i wanna filter out todo/queued-at too
    })

(defn widget-bar [{:keys [comp label opts initial-show]}]
  (let [show? (uix/state initial-show)]
    [:div {:class ["flex flex-col"]}
     [:div
      {:class ["flex flex-row"
               "items-center"
               "bg-city-orange-900"
               "sticky" "top-0"]}
      [:span {:class ["font-nes"]} label]
      [:div
       {:class ["ml-auto"]}
       [components.actions/actions-list
        {:actions [{:action/on-click (fn [_] (swap! show? not))
                    :action/label    "Show"
                    :action/icon     octicons/chevron-down16
                    :action/disabled @show?}
                   {:action/on-click (fn [_] (swap! show? not))
                    :action/label    "Hide"
                    :action/icon     octicons/chevron-up16
                    :action/disabled (not @show?)}]}]]]
     (when @show? [comp opts])]))

(defn widget [opts]
  [:div
   {:class ["text-city-pink-200"]}

   [:div
    {:class ["relative"]}

    [widget-bar {:comp  (fn [_opts]
                          [:div
                           [ingest/ingest-buttons]
                           [ingest/commit-ingest-buttons (:conn opts)]])
                 :opts  opts
                 :label :ingestors}]

    [widget-bar {:comp         focus/widget
                 :opts         (assoc opts :only-current-stack true)
                 :label        :current
                 :initial-show true}]
    [widget-bar {:comp         focus/widget
                 :opts         opts
                 :label        :focus
                 :initial-show true}]
    [widget-bar {:comp  blog/widget
                 :opts  opts
                 :label :blog}]]

   (let [queued-todos  (ui.db/queued-todos (:conn opts))
         recent-events (ui.db/events (:conn opts))

         event-filter-results
         (components.filter/use-filter
           (assoc filter-defs/fg-config
                  :items recent-events
                  :presets
                  {:default {:filters  #{}
                             :group-by :tags}}))
         all-todos (ui.db/garden-todos
                     (:conn opts) {:n 200 ;; TODO support fetching more
                                   :filter-pred
                                   (fn [{:org/keys [status]}]
                                     (#{:status/not-started} status))})
         todo-filter-results
         (components.filter/use-filter
           (assoc filter-defs/fg-config
                  :items all-todos
                  :presets
                  {:default {:filters  all-todos-initial-filters
                             :group-by :tags}}))]
     [:div
      [:div
       [:div "Todo list (" (count queued-todos) ")"]
       [components.todo/todo-list {:n 5} queued-todos]]

      (let [expanded (uix/state
                       ;; default to hiding all todos if some are queued
                       (< (count queued-todos) 2))]
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
             (:filtered-items todo-filter-results)]])])

      (let [expanded (uix/state nil)]
        [:div
         [:div "Events Cluster (" (count (:filtered-items event-filter-results)) ")"]
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
             (:filtered-items event-filter-results)]])])])])
