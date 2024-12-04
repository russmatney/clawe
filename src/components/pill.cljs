(ns components.pill
  (:require
   [uix.core :as uix :refer [$ defui]]
   ))

(defui pill [{:keys [label on-click active]}]
  ($ :div
     {:class
      ["flex" "justify-center"
       (if active
         "bg-yo-blue-900"
         "bg-yo-blue-700")
       (if active
         "bg-opacity-90"
         "bg-opacity-70")
       "rounded-xl"
       "px-3" "py-2"
       "text-sm"
       "font-mono"
       "cursor-pointer"
       (if active
         "text-city-pink-400"
         "text-city-green-500")
       "hover:text-city-pink-500"
       "hover:bg-opacity-100"]
      :on-click on-click}
     ($ :span (str label))))


(defui cluster [{:keys [pills]}]
  ($ :div
     {:class ["w-full"
              "max-w-xl"
              "mx-auto"
              "flex" "flex-row" "flex-wrap"
              "justify-center"]}
     (for [x pills]
       ($ pill (assoc x :key (str x))))))
