(ns hooks.screenshots
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.screenshots :as api.screenshots]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-screenshots []
  (api.screenshots/active-screenshots))

(defstream screenshots-stream [] api.screenshots/*screenshots-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-screenshots []
     (let [screenshots (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (swap! screenshots
                                (fn [_]
                                  (->> new-items (w/distinct-by :file/full-path)))))]

       (with-rpc [] (get-screenshots) handle-resp)
       (with-stream [] (screenshots-stream) handle-resp)

       {:items @screenshots})))
