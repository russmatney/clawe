(ns doctor.ui.views.commits
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.events :as components.events]
   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui widget [_opts]
  (let [events
        (->
          (hooks.use-db/use-query
            {:db->data
             (fn [db]
               (ui.db/events db {:event-types #{:type/commit}}))})
          :data)]

    ($ :div
       {:class ["flex" "flex-col" "flex-auto"
                "min-h-screen"
                "overflow-hidden"
                "bg-yo-blue-700"
                "text-white"]}

       (if (seq events)
         ($ components.events/events-cluster events)
         ($ :div "No commits found!")))))
