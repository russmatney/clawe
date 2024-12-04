(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   [taoensso.telemere :as t]
   #?@(:clj [[api.db :as api.db]]
       :cljs [[datascript.core :as d]
              [uix.core :as uix]
              [doctor.ui.hooks.plasma :refer [with-stream with-rpc]]
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
    (t/log! :info "get-db handler firing first batch")
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
   (defn use-db [{:keys [conn]}]
     (let [handle-resp
           (fn [items]
             (when conn
               (d/transact! conn items))

             (when conn
               (->> items
                    (map :e)
                    distinct
                    (map #(d/entity @conn %))
                    (map :doctor/type)
                    frequencies
                    (str "received data: " "datoms: " (count items) " ")
                    (t/log! :info)))

             (->> items (take 2)
                  (map (fn [dt] [(:a dt) (:v dt)]))
                  (t/log! :info))

             (-> (d/empty-db schema)
                 (d/db-with items)
                 ((fn [db]
                    (->>
                      (d/datoms db :eavt)
                      (map :e)
                      (distinct)
                      (take 1)
                      (d/pull-many db '[*]))))
                 (->>
                   (map (fn [x]
                          (t/log!
                            :info
                            (->>
                              x
                              (take 3)
                              (into {})))))
                   doall))
             )]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)

       {:db @conn})
     ))
