(ns doctor.ui.views.today
  (:require
   [tick.core :as t]
   [uix.core :as uix :refer [defui $]]

   [components.filter :as components.filter]
   [components.filter-defs :as filter-defs]
   [components.events :as components.events]
   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]))


(defn presets []
  {:some-preset
   {:filters     #{}
    :group-by    :filters/event-timestamp
    :sort-groups :filters/event-timestamp
    :default     true}})

(defui widget [opts]
  (let [today (t/today)
        events
        (:data
         (hooks.use-db/use-query
           {:db->data
            (fn [db]
              (ui.db/events db {:filter-by
                                #(some-> % :event/timestamp t/date (= today))}))}))

        notes
        (:data
         (hooks.use-db/use-query
           {:db->data
            (fn [db]
              (ui.db/garden-notes db
                                  {:filter-by
                                   #(some-> % :file/last-modified t/date (= today))}))}))

        filter-data
        (components.filter/use-filter
          (assoc filter-defs/fg-config
                 :id (:filter-id opts :views-today)
                 :presets (presets)
                 :items (concat events notes)
                 :label (str (count events) " events today")))]
    ($ :div
       {:class ["p-4" "text-slate-200"]}
       ($ :div
          (:filter-grouper filter-data)

          ($ components.filter/items-by-group
             (assoc filter-data :group->comp #($ components.events/event-clusters {:events (:group %)})))))))
