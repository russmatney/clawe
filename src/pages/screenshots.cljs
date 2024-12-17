(ns pages.screenshots
  (:require
   [uix.core :as uix :refer [defui $]]

   [components.screenshot :as screenshot]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.db :as ui.db]

   [doctor.ui.handlers :as handlers]
   [doctor.ui.views.ingest :as ingest]
   ))

(defui page [_opts]
  (let [{items :data}
        (hooks.use-db/use-query
          {:db->data #(ui.db/events % {:n           30
                                       :event-types #{:type/screenshot
                                                      :type/clip}})})]
    ($ :div
       {:class ["flex" "flex-row" "flex-wrap"
                "overflow-hidden"
                "bg-yo-blue-700"]}
       ($ ingest/ingest-buttons
          {:actions
           [{:action/label    "Ingest screenshots"
             :action/on-click (fn [_] (handlers/ingest-screenshots))}
            {:action/label    "Ingest clips"
             :action/on-click (fn [_] (handlers/ingest-clips))}]})

       (for [[i it] (->> items (map-indexed vector))]
         ($ screenshot/screenshot-comp {:key i :item it})))))
