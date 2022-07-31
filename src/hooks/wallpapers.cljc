(ns hooks.wallpapers
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[wallpapers.core :as wallpapers]
             [api.wallpapers]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-wallpapers []
  (api.wallpapers/active-wallpapers))

(defstream wallpapers-stream [] api.wallpapers/*wallpapers-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; state helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-wallpapers []
     (let [wallpapers  (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (swap! wallpapers
                                (fn [_]
                                  (println (some-> new-items first))
                                  (->> new-items (w/distinct-by :wallpaper/full-path)))))]

       (with-rpc [] (get-wallpapers) handle-resp)
       (with-stream [] (wallpapers-stream) handle-resp)
       {:items @wallpapers})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler set-wallpaper [item]
  (wallpapers/set-wallpaper item)
  (api.wallpapers/update-wallpapers))


;; could move toward ->actions being clj + cljs
#?(:cljs
   (defn ->actions [item]
     (when (= (:doctor/type item) :type/wallpaper)
       (let [{:keys []} item]
         (->>
           [{:action/label    "js/alert"
             :action/on-click #(js/alert item)}
            {:action/label    "Set as background"
             :action/on-click #(set-wallpaper item)}]
           (remove nil?))))))
