(ns pages.garden
  (:require
   [uix.core :as uix :refer [defui $]]

   [components.garden :as components.garden]
   [doctor.ui.db :as ui.db]))

(defui page [{:keys [conn]}]
  (let [items                   (ui.db/garden-notes conn {:n 500})
        [selected set-selected] (uix/use-state (first items)) ]
    ($ :div
       {:class ["flex" "flex-col" "flex-wrap"
                "overflow-hidden"
                "min-h-screen"]}

       (when-not items
         ($ :div
            {:class ["p-6" "text-lg" "text-white"]}
            "Loading...................."))

       (when selected
         ($ components.garden/selected-node selected))

       ($ :div
          {:class ["flex" "flex-row" "flex-wrap"
                   "justify-center"]}
          (for [[i it] (->> items (map-indexed vector))]
            ($ components.garden/garden-node
               {:key          i
                :on-select    (fn [_] (set-selected it))
                :is-selected? (= selected it)}
               (assoc it :index i)))))))
