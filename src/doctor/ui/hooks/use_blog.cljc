(ns doctor.ui.hooks.use-blog
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.blog]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

(defhandler get-blog-data [] (api.blog/build-blog-data))
(defstream blog-data-stream [] api.blog/*blog-data-stream*)

#?(:cljs
   (defn use-blog-data []
     (let [blog-data   (plasma.uix/state [])
           handle-resp #(reset! blog-data %)]

       (with-rpc [] (get-blog-data) handle-resp)
       (with-stream [] (blog-data-stream) handle-resp)

       blog-data)))

(defhandler republish-all [] (api.blog/republish-all))
