(ns hooks.garden
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[garden.core :as garden]
             [util]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))


(defhandler get-garden-handler []
  (->>
    (garden/get-garden)
    (map util/drop-complex-types)))

(defstream garden-stream [] garden/*garden-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; open-in-emacs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler open-in-emacs [item]
  (println "open in emacs!!!")
  (println "opening file:" item)
  :ok
  )


(comment
  (open-in-emacs {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   ;; TODO dry up vs views/garden.cljc
   (defn use-garden []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-garden-handler) handle-resp)
       (with-stream [] (garden-stream) handle-resp)

       {:items @items})))
