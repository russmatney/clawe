(ns doctor.ui.todos
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[doctor.api.todos :as d.todos]]
       :cljs [[uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]
              [tick.core :as t]
              [hiccup-icons.fa :as fa]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todos data api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-todos-handler [] (d.todos/get-todos))
(defstream todos-stream [] d.todos/*todos-stream*)

#?(:cljs
   (defn use-todos []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-todos-handler) handle-resp)
       (with-stream [] (todos-stream) handle-resp)

       {:items     @items
        :db-todos  (->> @items
                        (filter :db/id))
        :org-todos (->> @items
                        (remove :db/id))})))
