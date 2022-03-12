(ns doctor.ui.events
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[doctor.api.events :as api.events]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-events []
  (println "get-events handler firing")
  (api.events/recent-events))

(defstream events-stream [] api.events/*events-stream*)

#?(:clj
   (comment
     (api.events/re-push-events)
     ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-events []
     (let [evts        (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (println "new events!" new-items)
                         (swap! evts
                                (fn [_]
                                  (->> new-items
                                       ;; TODO this is not right
                                       (w/distinct-by :events/timestamp)))))]

       (with-rpc [] (get-events) handle-resp)
       (with-stream [] (events-stream) handle-resp)

       {:items @evts})))
