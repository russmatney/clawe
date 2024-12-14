(ns doctor.ui.hooks.use-db
  (:require
   [plasma.core :refer [defhandler defstream]]
   [taoensso.telemere :as t]
   #?@(:clj [[api.db :as api.db]]
       :cljs [
              [datascript.core :as d]
              [uix.core :as uix]
              [reagent.core :as r]

              [db.schema :refer [schema]]
              [doctor.ui.hooks.plasma :refer [with-stream with-rpc]]
              [doctor.ui.hooks.use-reaction :refer [use-reaction]]
              ])))

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


;; #?(:cljs (defonce conn-ratom (r/atom (d/create-conn schema))))

#?(:cljs (defonce conn (d/create-conn schema)))

#?(:cljs
   (defn use-query [{:keys [q conn->result]}]
     (let [[result set-result] (uix/use-state nil)
           ->result            (cond
                                 conn->result (fn [] (conn->result conn))
                                 q            (fn [] (->> (d/q q @conn) (map first))))
           ]
       ;; run in use-effect?
       (uix/use-effect
         (fn []
           (set-result (->result))
           (d/listen! conn :todos (fn [_tx] (set-result (->result)))))
         [])
       {:data     result
        :loading? (nil? result)})))

#?(:cljs
   (defn use-db []
     (let [handle-resp
           (fn [datoms]
             (when conn
               (d/transact! conn datoms))

             (when conn
               (->> datoms
                    (map :e)
                    distinct
                    (map #(d/entity @conn %))
                    (map :doctor/type)
                    frequencies
                    (#(t/log! {:data %} "Received data"))))

             ;; (->> datoms (take 2)
             ;;      (map (fn [dt] [(:a dt) (:v dt)]))
             ;;      (t/log! :info))

             ;; (-> (d/empty-db schema)
             ;;     (d/db-with datoms)
             ;;     ((fn [db]
             ;;        (->>
             ;;          (d/datoms db :eavt)
             ;;          (map :e)
             ;;          (distinct)
             ;;          (take 1)
             ;;          (d/pull-many db '[*]))))
             ;;     (->>
             ;;       (map (fn [x]
             ;;              (t/log!
             ;;                :info
             ;;                (->>
             ;;                  x
             ;;                  (take 3)
             ;;                  (into {})))))
             ;;       doall))
             )]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)

       {:conn conn})))
