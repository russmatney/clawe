(ns components.wallpaper
  (:require
   [uix.core.alpha :as uix]
   [tick.core :as t]
   [hooks.wallpapers :as hooks.wallpaper]))

(defn wallpaper-comp
  ([item] (wallpaper-comp nil item))
  ([_opts item]
   (let [{:keys [#_name
                 db/id
                 #_wallpaper/full-path
                 wallpaper/short-path
                 file/web-asset-path
                 wallpaper/last-time-set
                 wallpaper/used-count
                 ]} item
         hovering?  (uix/state false)]
     [:div
      {:class
       ["m-1" "p-4"
        "border" "border-city-blue-600"
        "bg-yo-blue-700"
        "text-white"]
       :on-mouse-enter #(do (reset! hovering? true))
       :on-mouse-leave #(do (reset! hovering? false))}
      (when web-asset-path
        [:img {:src   web-asset-path
               :class ["max-w-xl"
                       "max-h-72"]}])

      [:div {:class ["font-nes" "text-lg"]} short-path]
      [:div {:class ["text-lg"]} id]
      (when last-time-set
        [:div {:class ["text-lg"]}
         (t/instant (t/new-duration last-time-set :millis))])

      (when used-count
        [:div {:class ["text-lg"]} used-count])

      [:div.my-3
       (for [ax (hooks.wallpapers/->actions item)]
         ^{:key (:action/label ax)}
         [:div
          {:class    ["cursor-pointer"
                      "hover:text-yo-blue-300"]
           :on-click (:action/on-click ax)}
          (:action/label ax)])]])))
