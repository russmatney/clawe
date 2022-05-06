(ns components.count
  (:require
   [uix.core.alpha :as uix]))

(defn count-comp
  ([item] (count-comp nil item))
  ([_opts item]
   (let [{:count/keys [id value label color]
          :org/keys   [source-file body]}
         item
         label     (or label id)
         hovering? (uix/state false)]
     [:div
      {:class          ["m-1" "p-4"
                        "border" "border-city-blue-600"
                        "bg-yo-blue-700"
                        "text-white"
                        (when @hovering? "cursor-pointer")]
       :on-mouse-enter #(reset! hovering? true)
       :on-mouse-leave #(reset! hovering? false)}

      [:div
       {:class ["font-nes"
                "flex"
                "justify-center"]
        :style (when color {:color color})}
       value]

      [:div
       {:class ["font-mono" "text-lg"
                (when @hovering? "text-city-blue-400")]
        :style (when color {:color color})}
       label]

      (when @hovering?
        [:div
         {:class    ["font-mono"
                     "hover:text-city-blue-400"]
          :on-click (fn [_] (println "clicked"))}
         source-file])

      (when @hovering?
        [:div
         {:class ["font-mono" "text-city-blue-400"
                  "flex" "flex-col"
                  "p-2"
                  "bg-yo-blue-500"]}
         (for [[i line] (map-indexed vector body)]
           ^{:key i}
           (let [{:keys [text]} line]
             (cond
               (= "" text) [:span {:class ["py-1"]} " "]

               :else
               [:span text])))])])))
