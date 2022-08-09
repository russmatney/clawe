(ns pages.garden
  (:require
   [components.garden :as components.garden]
   [uix.core.alpha :as uix]
   [doctor.ui.db :as ui.db]))

(defn page [{:keys [conn]}]
  (let [items    (ui.db/garden-notes conn {:n 500})
        selected (uix/state (first items)) ]
    [:div
     {:class ["flex" "flex-col" "flex-wrap"
              "overflow-hidden"
              "min-h-screen"]}

     (when-not items
       [:div
        {:class ["p-6" "text-lg" "text-white"]}
        "Loading...................."])

     (when @selected
       (components.garden/selected-node @selected))

     [:div
      {:class ["flex" "flex-row" "flex-wrap"
               "justify-center"]}
      (for [[i it] (->> items (map-indexed vector))]
        ^{:key i}
        [components.garden/garden-node
         {:on-select    (fn [_] (reset! selected it))
          :is-selected? (= @selected it)}
         (assoc it :index i)])]]))
