(ns doctor.ui.hooks.use-topbar
  (:require
   [tick.core :as t]
   [uix.core :as uix]

   [doctor.ui.hooks.use-timer :refer [use-interval]]
   [hooks.topbar :as hooks.topbar]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar widget and state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn use-topbar-state []
  (let [[hovered-client set-hovered-client]                 (uix/use-state nil)
        [hovered-workspace set-hovered-workspace]           (uix/use-state nil)
        [last-hovered-client set-last-hovered-client]       (uix/use-state nil)
        [last-hovered-workspace set-last-hovered-workspace] (uix/use-state nil)
        [topbar-above set-topbar-above]                     (uix/use-state true)
        toggle-above-below                                  (fn []
                                                              (-> (hooks.topbar/toggle-topbar-above (not topbar-above))
                                                                  (.then (fn [v] (set-topbar-above v)))))
        ;; time                                                (use-interval {:->value  t/zoned-date-time
        ;;                                                                    :interval 1000})
        ]

    {:hovered-client         hovered-client
     :hovered-workspace      hovered-workspace
     :last-hovered-workspace last-hovered-workspace
     :last-hovered-client    last-hovered-client
     :on-hover-workspace     (fn [w]
                               (set-last-hovered-workspace w)
                               (set-hovered-workspace w)
                               ;; (pull-above)
                               )
     :on-unhover-workspace   (fn [_] (set-hovered-workspace nil))
     :on-hover-client        (fn [c]
                               (set-last-hovered-client c)
                               (set-hovered-client c)
                               ;; (pull-above)
                               )
     :on-unhover-client      (fn [_] (set-hovered-client nil))
     :topbar-above           topbar-above
     :toggle-above-below     toggle-above-below
     ;; :time                   time
     }))
