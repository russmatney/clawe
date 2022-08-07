(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.db :as api.db]]
       :cljs [[datascript.core :as d]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-db []
  (api.db/datoms-for-frontend))

(defstream db-stream [] api.db/*db-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-db []
     (let [conn       (plasma.uix/state nil)
           handle-get (fn [items]
                        (println "creating new db from items"
                                 (count items))
                        (->
                          (d/empty-db)
                          (d/db-with items)
                          (#(reset! conn %))))

           handle-stream (fn [datoms]
                           (println "datoms via stream"
                                    (count datoms))
                           (when @conn
                             (d/transact! conn datoms)))]

       (with-rpc [] (get-db) handle-get)
       (with-stream [] (db-stream) handle-stream)


       {:conn @conn})))
