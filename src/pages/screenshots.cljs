(ns pages.screenshots
  (:require
   [uix.core :as uix :refer [defui $]]

   [components.screenshot :as screenshot]
   [doctor.ui.db :as ui.db]))

(defui page [{:keys [conn]}]
  (let [items (ui.db/events conn {:n           30
                                  :event-types #{:type/screenshot
                                                 :type/clip}})]
    ($ :div
       {:class ["flex" "flex-row" "flex-wrap"
                "overflow-hidden"
                "bg-yo-blue-700"]}
       (for [[i it] (->> items (map-indexed vector))]
         ($ screenshot/screenshot-comp {:key i :item it})))))
