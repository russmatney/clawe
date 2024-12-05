(ns doctor.ui.views.commits
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.events :as components.events]
   [doctor.ui.db :as ui.db]))

(def icon nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui widget [{:keys [conn]}]
  (let [events (ui.db/events conn {:event-types #{:type/commit}})]

    ($ :div
       {:class ["flex" "flex-col" "flex-auto"
                "min-h-screen"
                "overflow-hidden"
                "bg-yo-blue-700"
                "text-white"]}

       (if (seq events)
         ($ components.events/events-cluster events)
         ($ :div "No commits found!")))))
