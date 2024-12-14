(ns components.wallpaper
  (:require
   [tick.core :as t]
   [uix.core :as uix :refer [$ defui]]

   [components.debug :as components.debug]
   [doctor.ui.actions :as actions]))

(defui wallpaper-comp
  [{:keys [item] :as _opts}]
  (let [{:keys [db/id
                wallpaper/short-path
                file/web-asset-path
                wallpaper/last-time-set
                wallpaper/used-count
                ]}                item
        [_hovering? set-hovering] (uix/use-state false)]
    ($ :div
       {:class
        ["m-1" "p-4"
         "border" "border-city-blue-600"
         "bg-yo-blue-700"
         "text-white"]
        :on-mouse-enter #(do (set-hovering true))
        :on-mouse-leave #(do (set-hovering false))}
       (when web-asset-path
         ($ :img {:src   web-asset-path
                  :class ["max-w-xl"
                          "max-h-72"]}))

       ($ :div {:class ["font-nes" "text-lg"]} short-path)
       ($ :div {:class ["text-lg"]} id)
       (when last-time-set
         ($ :div {:class ["text-lg"]}
            (str (t/instant (t/new-duration last-time-set :millis)))))

       (when used-count
         ($ :div {:class ["text-lg"]} used-count))

       ($ components.debug/raw-metadata {:label "Raw" :data item})

       ($ :div.my-3
          (for [ax (actions/->actions item)]
            ($ :div
               {:key      (:action/label ax)
                :class    ["cursor-pointer"
                           "hover:text-yo-blue-300"]
                :on-click (:action/on-click ax)}
               (:action/label ax)))))))
