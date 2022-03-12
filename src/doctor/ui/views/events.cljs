(ns doctor.ui.views.events
  (:require
   [doctor.ui.events :as events]
   [uix.core.alpha :as uix]))

(defn event-comp [opts it]
  [:div "event" (str it)])


(defn event-page []
  (let [{:keys [items]} (events/use-events)]
    (println "event-page" items)
    [:div
     {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}
     [:div
      [:h1 "Events!!!"]]
     [:div
      [:h1 (->> items count)]]

     (doall
       (for [[i it] (->> items (map-indexed vector))]
         ^{:key i}
         [event-comp nil it]))]))
