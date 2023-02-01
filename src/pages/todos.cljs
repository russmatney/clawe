(ns pages.todos
  (:require
   [uix.core.alpha :as uix]
   [components.todo :as components.todo]
   [components.filter :as components.filter]
   [components.filter-defs :as filter-defs]
   [doctor.ui.db :as ui.db]
   [doctor.ui.views.ingest :as ingest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn]}]
  (let [todos    (ui.db/garden-todos conn {:n 1000})
        selected (uix/state (first todos))
        {:keys [filtered-item-groups filtered-items filter-grouper]}
        (components.filter/use-filter
          (assoc filter-defs/fg-config :items todos))]
    [:div
     {:class ["grid" "grid-flow-row" "place-items-center"
              "overflow-hidden"
              "min-h-screen"
              "text-city-pink-200"]}

     [ingest/ingest-buttons]

     [:div {:class ["p-4"]} filter-grouper]

     [:div
      {:class ["grid" "grid-flow-row"]}

      [components.todo/todo-list
       {:label     "In Progress"
        :on-select (fn [it] (reset! selected it))
        :selected  @selected}
       (->> #_todos
            filtered-items
            (filter
              (fn [todo]
                (or
                  (-> todo :org/status #{:status/in-progress})
                  (and
                    (-> todo :todo/queued-at)
                    (not
                      (-> todo :org/status #{:status/cancelled
                                             :status/done})))))))]

      (for [[i {:keys [item-group label]}]
            (->> filtered-item-groups
                 (sort-by :label >)
                 (map-indexed vector))]
        ^{:key i}
        [components.todo/todo-list {:label     (str label " (" (count item-group) ")")
                                    :on-select (fn [it] (reset! selected it))
                                    :selected  @selected
                                    :n         9}
         item-group])]]))
