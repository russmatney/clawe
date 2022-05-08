(ns components.floating
  (:require
   [uix.core.alpha :as uix]
   ["@floating-ui/react-dom-interactions" :as FUI]))

(defn popover [opts]
  (let [{:keys [anchor-comp popover-comp]} opts
        open?                              (uix/state false)

        floating-state (FUI/useFloating
                         #js {:open @open? :onOpenChange #(reset! open? %)})
        context        (. floating-state -context)
        ixs            (FUI/useInteractions
                         #js [(FUI/useHover context)
                              (FUI/useClick context)
                              (FUI/useDismiss context)])]

    [:div
     [:div (js->clj (.getReferenceProps ixs (clj->js {:ref (.-reference floating-state)})))
      anchor-comp]

     [:> FUI/FloatingPortal
      (when @open?
        [:> FUI/FloatingFocusManager {:context context}
         [:div (js->clj (.getFloatingProps
                          ixs (clj->js {:ref   (.-floating floating-state)
                                        :style {:position (.-strategy floating-state)
                                                :top      (or (.-y floating-state) "")
                                                :left     (or (.-x floating-state) "")}})))
          popover-comp]])]]))
