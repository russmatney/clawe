(ns doctor.ui.hooks.use-blog
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.blog]]
       :cljs [[uix.core :as uix]
              [doctor.ui.hooks.plasma :refer [with-stream with-rpc]]
              ])))

(defhandler get-blog-data [] (api.blog/build-blog-data))
(defstream blog-data-stream [] api.blog/*blog-data-stream*)

#?(:cljs
   (defn use-blog-data []
     (let [[blog-data set-blog-data] (uix/use-state [])]

       (with-rpc [] (get-blog-data) set-blog-data)
       (with-stream [] (blog-data-stream) set-blog-data)

       blog-data)))

(defhandler rebuild-all [] (api.blog/rebuild-all))
