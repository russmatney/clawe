(ns pages.wallpapers
  (:require
   [hooks.wallpapers]
   [components.wallpaper]))

(defn widget []
  (let [{:keys [items]} (hooks.wallpapers/use-wallpapers)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              ]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [components.wallpaper/wallpaper-comp nil it])]))
