(ns components.floating
  (:require
   [uix.core.alpha :as uix]
   ["@floating-ui/react-dom-interactions" :as FUI]))

(defn popover [opts]
  (let [{:keys [click hover offset
                anchor-comp
                anchor-comp-props
                popover-comp]} opts
        offset                 (or offset 30)
        open                   (uix/state false)

        floating-state (FUI/useFloating
                         (clj->js {:open @open :onOpenChange #(reset! open %)
                                   :middleware
                                   [(FUI/offset offset)
                                    (FUI/flip)
                                    (FUI/shift)]}))
        context        (. floating-state -context)
        ixs            (FUI/useInteractions
                         (clj->js
                           (->> [(when hover (FUI/useHover context
                                                           #js {:restMs 150
                                                                :delay  #js {:open 300}}))
                                 (when click (FUI/useClick context))
                                 (FUI/useDismiss context)]
                                (remove nil?)
                                (into []))))]

    [:<>
     [:div
      (merge
        (js->clj (.getReferenceProps ixs (clj->js {:ref (.-reference floating-state)})))
        anchor-comp-props
        )
      anchor-comp]

     [:> FUI/FloatingPortal
      (when @open
        [:> FUI/FloatingFocusManager {:context context}
         [:div (js->clj (.getFloatingProps
                          ixs (clj->js {:ref   (.-floating floating-state)
                                        :style {:position (.-strategy floating-state)
                                                :top      (or (.-y floating-state) "")
                                                :left     (or (.-x floating-state) "")}})))
          popover-comp]])]]))
