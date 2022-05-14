(ns hooks.repos
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.repos]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-repos []
  (let [evts (api.repos/active-repos)]
    evts))

(defstream repos-stream [] api.repos/*repos-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-repos []
     (let [repos       (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (println "new repos!" (count new-items))
                         (swap! repos
                                (fn [_]
                                  (->> new-items
                                       ;; TODO this is not right
                                       ;; (w/distinct-by :events/timestamp)
                                       ))))]

       (with-rpc [] (get-repos) handle-resp)
       (with-stream [] (repos-stream) handle-resp)

       {:repos @repos})))
