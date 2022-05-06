(ns components.actions
  (:require
   [components.icons :as icons]))

(defn action-list [actions]
  (when actions
    [:div
     {:class ["flex" "flex-row" "flex-wrap"]}
     (for [[i ax] (map-indexed vector actions)]
       ^{:key i}
       [icons/action-icon ax])]))
