(ns doctor.ui.components.debug
  (:require
   [uix.core.alpha :as uix]))


(defn raw-metadata
  ([metadata] [raw-metadata nil metadata])
  ([{:keys [label initial-show?]} metadata]
   (let [label                    (or label "Show raw metadata")
         show-raw-metadata?       (uix/state initial-show?)
         toggle-show-raw-metadata #(swap! show-raw-metadata? (comp boolean not))]
     [:div.text-right
      [:span.text-sm
       {:class    ["hover:text-city-pink-400" "cursor-pointer"]
        :on-click toggle-show-raw-metadata}
       label]

      (when @show-raw-metadata?
        [:div
         {:class ["mt-auto"]}
         (when metadata
           (->>
             metadata
             (map (fn [[k v]]
                    ^{:key k}
                    [:div.font-mono "[" (str k) " " (str v) "] "]))))])])))
