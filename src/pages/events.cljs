(ns pages.events
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.events :as components.events]
   [doctor.ui.db :as ui.db]
   [doctor.ui.views.ingest :as ingest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui page [{:keys [conn]}]
  (let [events (ui.db/events conn)]
    ($ :div
       {:class ["flex" "flex-col" "flex-auto"
                "min-h-screen"
                "overflow-hidden"
                "bg-yo-blue-700"]}
       ($ ingest/ingest-buttons)
       ($ ingest/commit-ingest-buttons conn)

       ($ components.events/events-cluster events))))
