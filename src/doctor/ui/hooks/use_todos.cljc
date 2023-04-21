(ns doctor.ui.hooks.use-todos
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.todos]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

(defhandler get-todos-data [] (api.todos/build-todos))
(defstream todos-stream [] api.todos/*todos-stream*)

#?(:cljs
   (defn use-todos-data []
     (let [todos       (plasma.uix/state [])
           handle-resp #(reset! todos %)]

       (with-rpc [] (get-todos-data) handle-resp)
       (with-stream [] (todos-stream) handle-resp)

       todos)))

(defhandler get-current-todos [] (api.todos/list-current-db-todos))
(defstream current-todos-stream [] api.todos/*current-todos-stream*)

#?(:cljs
   (defn use-current-todos []
     (let [todos       (plasma.uix/state [])
           handle-resp #(reset! todos %)]
       (with-rpc [] (get-current-todos) handle-resp)
       (with-stream [] (current-todos-stream) handle-resp)
       todos)))
