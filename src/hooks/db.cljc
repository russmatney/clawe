(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[manifold.stream :as s]
             [api.db :as api.db]]
       :cljs [[datascript.core :as d]
              [plasma.uix :as plasma.uix :refer [with-rpc with-stream]]
              [db.schema :refer [schema]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def datoms-in-first-batch 30000)
;; (def datoms-per-batch 50000)
;; (def n-batches 5)
;; TODO write smarter hooks/initial load queries for grabbing specific backend data
(defhandler get-db []
  (let [datoms      (api.db/datoms-for-frontend)
        first-batch (->> datoms (take datoms-in-first-batch))
        _rest       (->> datoms (drop datoms-in-first-batch))]
    ;; fire the rest in streamed batches

    ;; NOTE disabled for now - should move to requesting specific data
    #_(future
        (->> rest
             (partition-all datoms-per-batch)
             (take n-batches)
             (map (fn [batch]
                    (s/put! api.db/*db-stream* batch)))
             doall))
    first-batch))

(defstream db-stream [] api.db/*db-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-db []
     (let [conn        (plasma.uix/state nil)
           handle-resp (fn [items]
                         (println "new datoms" (count items))
                         (if @conn
                           (d/transact! conn items)
                           (-> (d/empty-db schema)
                               (d/db-with items)
                               (#(reset! conn %)))))]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)


       {:conn @conn})))
