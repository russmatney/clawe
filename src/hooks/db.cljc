(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   [taoensso.timbre :as log]
   #?@(:clj [[api.db :as api.db]]
       :cljs [[datascript.core :as d]
              [plasma.uix :as plasma.uix :refer [with-rpc with-stream]]
              [db.schema :refer [schema]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defstream db-stream [] api.db/*db-stream*)

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
           (map api.db/push-to-fe-db)
           doall))

    first-batch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-db []
     (let [conn        (plasma.uix/state nil)
           handle-resp (fn [items]
                         (if @conn
                           (d/transact! conn items)
                           (-> (d/empty-db schema)
                               (d/db-with items)
                               (#(reset! conn %))))

                         (->> items
                              (map :e)
                              distinct
                              (map #(d/entity @conn %))
                              (map :doctor/type)
                              frequencies
                              (log/info "received data: "
                                        "datoms: " (count items)))

                         (->> items (take 5)
                              (map (fn [dt] [(:a dt) (:v dt)]))
                              (log/info)))]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)


       {:conn @conn})))
