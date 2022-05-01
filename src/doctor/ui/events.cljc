(ns doctor.ui.events
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[doctor.api.events :as api.events]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-events []
  (println "get-events handler firing")
  (let [evts (api.events/recent-events)]
    (println "returning evts in handler" (count evts))
    evts))

(defstream events-stream [] api.events/*events-stream*)

#?(:clj
   (comment
     (api.events/re-push-events)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-events []
     (let [evts        (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (println "new events!" (count new-items))
                         (swap! evts
                                (fn [_]
                                  (->> new-items
                                       ;; TODO this is not right
                                       ;; (w/distinct-by :events/timestamp)
                                       ))))]

       (with-rpc [] (get-events) handle-resp)
       (with-stream [] (events-stream) handle-resp)

       {:items @evts})))
