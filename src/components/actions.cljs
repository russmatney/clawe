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

(defn actions-popup [actions]
  [floating/popover
   {:hover true :click true
    :anchor-comp
    [:div "Actions"]
    :popover-comp
    [:div
     {:class ["bg-city-blue-400"]}
     [actions-list actions]]}])
