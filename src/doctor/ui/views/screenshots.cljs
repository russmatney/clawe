(ns doctor.ui.views.screenshots
  (:require
   [doctor.ui.screenshots :as screenshots]
   [uix.core.alpha :as uix]))

(defn ->actions [item]
  (let [{:keys []} item]
    (->>
      [{:action/label    "js/alert"
        :action/on-click #(js/alert item)}]
      (remove nil?))))

(defn screenshot-comp
  ([item] (screenshot-comp nil item))
  ([_opts item]
   (let [{:keys [;; name file/full-path
                 file/web-asset-path]} item
         hovering?                     (uix/state false)]

     [:div
      {:class
       ["m-1" "p-4"
        "border" "border-city-blue-600"
        "bg-yo-blue-700"
        "text-white"]
       :on-mouse-enter #(do (reset! hovering? true))
       :on-mouse-leave #(do (reset! hovering? false))}
      (when web-asset-path
        [:img {:src web-asset-path}])

      [:div
       (for [ax (->actions item)]
         ^{:key (:action/label ax)}
         [:div
          {:class    ["cursor-pointer"
                      "hover:text-yo-blue-300"]
           :on-click (:action/on-click ax)}
          (:action/label ax)])]])))

(defn widget []
  (let [{:keys [items]} (screenshots/use-screenshots)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              ]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [screenshot-comp nil it])]))
