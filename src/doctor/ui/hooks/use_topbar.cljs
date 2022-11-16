(ns doctor.ui.hooks.use-topbar
  (:require
   [tick.core :as t]
   [uix.core.alpha :as uix]
   [hooks.topbar :as hooks.topbar]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar widget and state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn use-topbar-state []
  (let [hovered-client         (uix/state nil)
        hovered-workspace      (uix/state nil)
        last-hovered-client    (uix/state nil)
        last-hovered-workspace (uix/state nil)
        topbar-above           (uix/state true)
        toggle-above-below     (fn []
                                 (-> (hooks.topbar/toggle-topbar-above (not @topbar-above))
                                     (.then (fn [v] (reset! topbar-above v)))))
        time                   (uix/state (t/zoned-date-time))
        interval               (atom nil)]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time
                                                #_{:clj-kondo/ignore [:invalid-arity]}
                                                (t/zoned-date-time)) 1000))
      (fn [] (js/clearInterval @interval)))

    {:hovered-client         @hovered-client
     :hovered-workspace      @hovered-workspace
     :last-hovered-workspace @last-hovered-workspace
     :last-hovered-client    @last-hovered-client
     :on-hover-workspace     (fn [w]
                               (reset! last-hovered-workspace w)
                               (reset! hovered-workspace w)
                               ;; (pull-above)
                               )
     :on-unhover-workspace   (fn [_] (reset! hovered-workspace nil))
     :on-hover-client        (fn [c]
                               (reset! last-hovered-client c)
                               (reset! hovered-client c)
                               ;; (pull-above)
                               )
     :on-unhover-client      (fn [_] (reset! hovered-client nil))
     :topbar-above           @topbar-above
     :toggle-above-below     toggle-above-below
     :time                   @time}))
