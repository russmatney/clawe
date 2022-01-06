(ns doctor.ui.views.wallpapers
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [clawe.wallpapers :as c.wallpapers]
             [doctor.api.wallpapers :as d.wallpapers]]
       :cljs [[wing.core :as w]
              [uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]
              [tick.alpha.api :as t]
              ])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-wallpapers []
  (d.wallpapers/active-wallpapers))

#?(:clj
   (defsys *wallpapers-stream*
     :start (s/stream)
     :stop (s/close! *wallpapers-stream*)))

(defstream wallpapers-stream [] *wallpapers-stream*)

#?(:clj
   (defn update-wallpapers []
     (println "pushing to wallpapers stream (updating wallpapers)!")
     (s/put! *wallpapers-stream* (d.wallpapers/active-wallpapers))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler set-wallpaper [item]
  (c.wallpapers/set-wallpaper item)
  (update-wallpapers))

#?(:cljs
   (defn ->actions [item]
     (let [{:keys []} item]
       (->>
         [{:action/label    "js/alert"
           :action/on-click #(js/alert item)}
          {:action/label    "Set as background"
           :action/on-click #(set-wallpaper item)}
          ]
         (remove nil?)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-wallpapers []
     (let [wallpapers  (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (swap! wallpapers
                                (fn [_]
                                  (println (some-> new-items first))
                                  (->> new-items (w/distinct-by :file/full-path)))))]

       (with-rpc [] (get-wallpapers) handle-resp)
       (with-stream [] (wallpapers-stream) handle-resp)
       {:items @wallpapers})))

#?(:cljs
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
             (:action/label ax)])]]))))

#?(:cljs
   (defn widget []
     (let [{:keys [items]} (use-wallpapers)]
       [:div
        {:class ["flex" "flex-row" "flex-wrap"
                 "min-h-screen"
                 "overflow-hidden"
                 "bg-yo-blue-700"
                 ]}
        (for [[i it] (->> items (map-indexed vector))]
          ^{:key i}
          [screenshot-comp nil it])])))
