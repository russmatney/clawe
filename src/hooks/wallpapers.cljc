(ns hooks.wallpapers
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[clawe.wallpapers :as c.wallpapers]
             [doctor.api.wallpapers :as d.wallpapers]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-wallpapers []
  (d.wallpapers/active-wallpapers))

(defstream wallpapers-stream [] d.wallpapers/*wallpapers-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler set-wallpaper [item]
  (c.wallpapers/set-wallpaper item)
  (d.wallpapers/update-wallpapers))



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
                                  (->> new-items (w/distinct-by :file/full-path)))))]

       (with-rpc [] (get-wallpapers) handle-resp)
       (with-stream [] (wallpapers-stream) handle-resp)
       {:items @wallpapers})))
