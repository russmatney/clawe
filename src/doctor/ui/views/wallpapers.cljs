(ns doctor.ui.views.wallpapers
  (:require
   [doctor.ui.wallpapers :as wallpapers]
   [uix.core.alpha :as uix]
   [tick.core :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->actions [item]
  (let [{:keys []} item]
    (->>
      [{:action/label    "js/alert"
        :action/on-click #(js/alert item)}
       {:action/label    "Set as background"
        :action/on-click #(wallpapers/set-wallpaper item)}
       ]
      (remove nil?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn screenshot-comp
  ([item] (screenshot-comp nil item))
  ([_opts item]
   (let [{:keys [#_name
                 db/id
                 file/full-path
                 file/common-path
                 file/web-asset-path
                 background/last-time-set
                 background/used-count
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

      [:div {:class ["font-nes" "text-lg"]} common-path]
      [:div {:class ["text-lg"]} id]
      (when last-time-set
        [:div {:class ["text-lg"]}
         (t/instant (t/new-duration last-time-set :millis))])

      (when used-count
        [:div {:class ["text-lg"]} used-count])

      [:div.my-3
       (for [ax (->actions item)]
         ^{:key (:action/label ax)}
         [:div
          {:class    ["cursor-pointer"
                      "hover:text-yo-blue-300"]
           :on-click (:action/on-click ax)}
          (:action/label ax)])]])))

(defn widget []
  (let [{:keys [items]} (wallpapers/use-wallpapers)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              ]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [screenshot-comp nil it])]))
