(ns doctor.ui.hooks.use-selection
  (:require
   [uix.core :as uix :refer []]))

(defn use-selection []
  (let [[val set-val]       (uix/use-state nil)
        on-selection-change (fn [_ev]
                              (set-val (str (js/window.getSelection))))]
    (uix/use-effect
      (fn []
        (js/document.addEventListener
          "selectionchange" on-selection-change)
        (fn []
          (js/document.removeEventListener
            "selectionchange" on-selection-change)))
      [val])
    val))

(defn last-n-selections
  ([] (last-n-selections 5))
  ([_n]
   (let [[vals set-vals] (uix/use-state [])
         on-selection-change
         (fn [_]
           ;; TODO keep last 10 ~unique~ selections
           (set-vals #(->> % (take 9)
                           (concat [(str (js/window.getSelection))])
                           (into #{})
                           (into []))))]
     (uix/use-effect
       (fn []
         (js/document.addEventListener
           "selectionchange" on-selection-change)
         (fn []
           (js/document.removeEventListener
             "selectionchange" on-selection-change)))
       [vals])
     vals)))
