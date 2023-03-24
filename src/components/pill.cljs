(ns components.pill)

(defn pill [{:keys [label on-click active]}]
  [:div
   {:class
    ["flex" "justify-center"
     "bg-yo-blue-800"
     "rounded-xl"
     "px-3" "py-2"
     "text-sm"
     "font-mono"
     "cursor-pointer"
     (if active
       "text-city-pink-400"
       "text-city-green-600")
     "hover:text-city-red-600"]
    :on-click on-click}
   [:span label]])


(defn cluster [xs]
  [:div
   {:class ["w-full"
            "flex" "flex-row" "flex-wrap"
            "justify-center"]}
   (for [x xs]
     ^{:key (str x)}
     [pill x])])
