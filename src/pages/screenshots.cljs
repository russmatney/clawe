(ns pages.screenshots
  (:require
   [components.screenshot :as screenshot]
   [doctor.ui.db :as ui.db]))

(defn page [{:keys [conn]}]
  (let [items (ui.db/events conn {:n           30
                                  :event-types #{:type/screenshot
                                                 :type/clip}})]
    [:div
     {:class ["flex" "flex-row" "flex-wrap"
              "overflow-hidden"
              "bg-yo-blue-700"]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [screenshot/screenshot-comp nil it])]))
