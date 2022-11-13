(ns doctor.ui.hooks.use-focus
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.focus]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

(defhandler get-focus-data [] (api.focus/build-focus-data))
(defstream focus-data-stream [] api.focus/*focus-data-stream*)

#?(:cljs
   (defn use-focus-data []
     (let [focus-data  (plasma.uix/state [])
           handle-resp #(reset! focus-data %)]

       (with-rpc [] (get-focus-data) handle-resp)
       (with-stream [] (focus-data-stream) handle-resp)

       focus-data)))