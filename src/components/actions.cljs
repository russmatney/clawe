(ns components.actions
  (:require
   [components.icons :as icons]
   [components.floating :as floating]))

(defn actions-list [actions]
  (when actions
    [:div
     {:class ["flex" "flex-row" "flex-wrap"]}
     (for [[i ax] (map-indexed vector actions)]
       ^{:key i}
       [icons/action-icon-button ax])]))

(defn actions-popup [opts-or-ax]
  (let [actions (:actions opts-or-ax opts-or-ax)]
    [floating/popover
     {:hover true :click true
      :anchor-comp
      (:comp opts-or-ax
             [:div (:label opts-or-ax "Actions")])
      :popover-comp
      [:div
       {:class ["bg-city-blue-400"]}
       [actions-list actions]]}]))
