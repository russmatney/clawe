(ns pages.events
  (:require
   [hooks.events]
   [components.debug]
   [components.chess]
   [components.git]
   [components.screenshot]
   [components.timeline]
   [components.todo]
   [components.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [{:keys [items]} (hooks.events/use-events)]
    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"]}

     [components.events/events-cluster nil items]]))
