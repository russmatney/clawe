(ns hooks.journals
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[garden.core :as garden]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-journals []
  (let [evts (garden/active-journals)]
    evts))

(defstream journals-stream [] garden/*journals-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-journals []
     (let [journals    (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (println "new journals!" (count new-items))
                         (reset! journals new-items))]

       (with-rpc [] (get-journals) handle-resp)
       (with-stream [] (journals-stream) handle-resp)

       {:items @journals})))
