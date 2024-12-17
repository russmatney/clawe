(ns pages.todos
  (:require
   [uix.core :as uix :refer [defui $]]

   [components.todo :as components.todo]
   [components.filter :as components.filter]
   [components.filter-defs :as filter-defs]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.db :as ui.db]
   [doctor.ui.views.ingest :as ingest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui page [_opts]
  (let [todos
        (-> (hooks.use-db/use-query
              {:db->data (fn [db] (ui.db/list-todos db {:n 1000}))})
            :data)
        [selected set-selected] (uix/use-state (first todos))
        {:keys [filtered-items filter-grouper]
         :as   filter-data}
        (components.filter/use-filter
          (assoc filter-defs/fg-config :items todos))]
    ($ :div
       {:class ["grid" "grid-flow-row" "place-items-center"
                "overflow-hidden"
                "min-h-screen"
                "text-city-pink-200"]}

       ($ ingest/ingest-buttons)

       ($ :div {:class ["p-4"]} filter-grouper)

       ($ :div
          {:class ["grid" "grid-flow-row"]}

          ($ components.todo/todo-list
             {:label     "In Progress"
              :on-select (fn [it] (set-selected it))
              :selected  selected
              :todos
              (->> #_todos
                   filtered-items
                   (filter
                     (fn [todo]
                       (or
                         (-> todo :org/status #{:status/in-progress})
                         (and
                           #_(-> todo :todo/queued-at) true
                           (not
                             (-> todo :org/status #{:status/cancelled
                                                    :status/done})))))))})

          ($ components.filter/items-by-group
             (assoc filter-data :item->comp #($ components.todo/todo-row %)))))))
