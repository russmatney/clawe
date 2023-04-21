(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   [taoensso.timbre :as log]
   #?@(:clj [[api.db :as api.db]
             [manifold.stream :as s]]
       :cljs [[datascript.core :as d]
              [plasma.uix :as plasma.uix :refer [with-rpc with-stream]]
              [db.schema :refer [schema]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defstream db-stream [] api.db/*db-stream*)

#?(:clj
   (defn push-to-fe-db [data]
     (log/info "pushing data to fe-db")
     (s/put! api.db/*db-stream* data)))


(def datoms-in-first-batch 30000)
(def datoms-per-batch 50000)
(def n-batches 5)

;; TODO write smarter hooks/initial load queries for grabbing specific backend data
(defhandler get-db []
  (let [datoms      (api.db/datoms-for-frontend)
        first-batch (->> datoms (take datoms-in-first-batch))
        rest-data   (->> datoms (drop datoms-in-first-batch))]
    (log/info "get-db handler firing first batch")
    ;; fire the rest in streamed batches

    ;; TODO refactor towards fetching on demand
    (future
      (->> rest-data
           (partition-all datoms-per-batch)
           (take n-batches)
           (map push-to-fe-db)
           doall))
    first-batch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-db []
     (let [conn        (plasma.uix/state nil)
           handle-resp (fn [items]
                         (log/info "new datoms" (count items))
                         (if @conn
                           (d/transact! conn items)
                           (-> (d/empty-db schema)
                               (d/db-with items)
                               (#(reset! conn %)))))]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)


       {:conn @conn})))
