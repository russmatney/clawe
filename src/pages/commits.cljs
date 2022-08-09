(ns pages.commits
  (:require
   [components.events :as components.events]
   [pages.events.db :as pages.events.db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn]}]
  (let [events (pages.events.db/db-events conn #{:type/commit})]

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     (if (seq events)
       [components.events/events-cluster nil events]
       [:div "No commits found!"])]))
