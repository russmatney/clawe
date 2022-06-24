(ns hooks.commits
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.events]
             [api.commits]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-commits []
  (api.commits/recent-commits))

(defstream commits-stream [] api.commits/*commits-stream*)

#?(:clj
   (comment
     (api.commits/re-push-commits)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-commits []
     (let [evts        (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (println "new commits!" (count new-items))
                         (swap! evts
                                (fn [_]
                                  (->> new-items
                                       ;; TODO this is not right
                                       ;; (w/distinct-by :commits/timestamp)
                                       ))))]

       (with-rpc [] (get-commits) handle-resp)
       (with-stream [] (commits-stream) handle-resp)

       {:items @evts})))
