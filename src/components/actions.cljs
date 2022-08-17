(ns components.actions
  (:require
   [components.icons :as icons]
   [components.floating :as floating]))

(defn actions-list [opts-or-axs]
  (let [actions (:actions opts-or-axs opts-or-axs)]
    (when actions
      [:div
       {:class ["grid" "grid-flow-col"]}
       (for [[i ax] (->> actions
                         (remove nil?)
                         (map-indexed vector))]
         ^{:key i}
         [icons/action-icon-button ax])])))

(defn actions-popup [opts]
  (let [actions (:actions opts opts)]
    [floating/popover
     {:hover  true :click true
      :offset 0
      :anchor-comp
      (:comp opts
             [:div (:label opts "Actions")])
      :popover-comp
      [:div
       {:class ["bg-city-blue-400"]}
       [actions-list opts]]}]))
