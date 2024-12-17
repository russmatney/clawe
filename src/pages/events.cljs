(ns pages.events
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.events :as components.events]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.db :as ui.db]
   [doctor.ui.views.ingest :as ingest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui page [_opts]
  (let [events (-> (hooks.use-db/use-query {:db->data ui.db/events}) :data)]
    ($ :div
       {:class ["flex" "flex-col" "flex-auto"
                "min-h-screen"
                "overflow-hidden"
                "bg-yo-blue-700"]}
       ($ ingest/ingest-buttons)
       ($ ingest/commit-ingest-buttons)

       ($ components.events/events-cluster events))))
