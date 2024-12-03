(ns doctor.ui.hooks.use-timer
  (:require
   [uix.core :as uix]))

(defn use-interval [{:keys [->value interval]}]
  (let [interval                    (or interval 500)
        [throttled-value set-value] (uix/use-state (->value))]
    (uix/use-effect
      (fn []
        (let [id (js/setInterval #(set-value (->value)) interval)]
          #(js/clearInterval id)))
      [->value])

    throttled-value))
