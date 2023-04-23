(ns doctor.ui.views.commits
  (:require
   [components.events :as components.events]
   [doctor.ui.db :as ui.db]
   [hiccup-icons.octicons :as octicons]))

(def icon octicons/commit)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget [{:keys [conn]}]
  (let [events (ui.db/events conn {:event-types #{:type/commit}})]

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     (if (seq events)
       [components.events/events-cluster nil events]
       [:div "No commits found!"])]))
