(ns hooks.garden
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[garden.core :as garden]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

(defhandler get-garden-handler []
  (garden/get-garden))

(defstream garden-stream [] garden/*garden-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; open-in-emacs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO refactor into another hook
(defhandler open-in-emacs [item]
  (println "open in emacs!!!")
  (println "opening file:" item)
  :ok)

(comment
  (open-in-emacs {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-garden []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-garden-handler) handle-resp)
       (with-stream [] (garden-stream) handle-resp)

       {:items @items})))
