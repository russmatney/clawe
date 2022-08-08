(ns hooks.db
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[manifold.stream :as s]
             [api.db :as api.db]
             [api.screenshots :as api.screenshots]
             [chess.db :as chess.db]]
       :cljs [[datascript.core :as d]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def datoms-in-first-batch 30000)
(def datoms-per-batch 50000)
(defhandler get-db []
  (let [datoms      (api.db/datoms-for-frontend)
        first-batch (->> datoms (take datoms-in-first-batch))
        rest        (->> datoms (drop datoms-in-first-batch))]
    ;; fire the rest in streamed batches
    (future
      (->> rest
           (partition-all datoms-per-batch)
           (map (fn [batch]
                  (s/put! api.db/*db-stream* batch)))
           doall))
    first-batch))

(defstream db-stream [] api.db/*db-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ingestors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-screenshots []
  (api.screenshots/ingest-screenshots)
  :ok)

(defhandler ingest-lichess-games []
  (chess.db/ingest-lichess-games)
  :ok)

(defhandler clear-lichess-games-cache []
  (chess.core/clear-cache)
  :ok)


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
                           (->
                             (d/empty-db)
                             (d/db-with items)
                             (#(reset! conn %)))))]

       (with-stream [] (db-stream) handle-resp)
       (with-rpc [] (get-db) handle-resp)


       {:conn @conn})))
