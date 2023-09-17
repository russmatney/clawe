(ns doctor.ui.hooks.use-selection
  (:require
   [uix.core.alpha :as uix]))

(defn use-selection []
  (let [val*    (uix/state nil)
        set-val (fn [_ev]
                  (reset! val* (str (js/window.getSelection))))]
    (uix/with-effect [@val*]
      (js/document.addEventListener
        "selectionchange" set-val)
      (fn []
        (js/document.removeEventListener
          "selectionchange" set-val)))
    @val*))

(def selections (atom []))

(defn last-n-selections
  ([] (last-n-selections 5))
  ([_n]
   (let [vals*   (uix/state @selections)
         set-val (fn [_]
                   ;; TODO keep last 10 ~unique~ selections
                   (swap! vals* #(->> % (take 9)
                                      (concat [(str (js/window.getSelection))])
                                      (into #{})
                                      (into [])))
                   (reset! selections @vals*))]
     (uix/with-effect [@vals*]
       (js/document.addEventListener
         "selectionchange" set-val)
       (fn []
         (js/document.removeEventListener
           "selectionchange" set-val)))
     @vals*)))
