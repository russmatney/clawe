(ns components.floating
  (:require
   [uix.core :as uix :refer [defui $]]
   ["@floating-ui/react-dom-interactions" :as FUI]))

(defui popover [opts]
  (let [{:keys [click hover offset
                anchor-comp
                anchor-comp-props
                popover-comp
                popover-comp-props
                ]}      opts
        offset          (or offset 30)
        [open set-open] (uix/use-state false)

        floating-state
        (FUI/useFloating
          (clj->js
            {:open open :onOpenChange set-open
             :middleware
             [(FUI/offset offset)
              (FUI/autoPlacement)
              ;; (FUI/flip)
              (FUI/size
                #js {:apply
                     (fn [ctx]
                       (let [availWidth  (if (> (.-availableWidth ctx) 0)
                                           (.-availableWidth ctx)
                                           200)
                             availHeight (if (> (.-availableHeight ctx) 0)
                                           (.-availableHeight ctx)
                                           200)]
                         (set! (.. ctx -elements -floating -style -maxHeight)
                               (str availHeight "px"))
                         (set! (.. ctx -elements -floating -style -maxWidth)
                               (str availWidth "px")))
                       )})
              (FUI/shift)]}))
        context (. floating-state -context)
        ixs     (FUI/useInteractions
                  (clj->js
                    (->> [(when hover (FUI/useHover context
                                                    #js {:restMs 150
                                                         :delay  #js {:open 300}}))
                          (when click (FUI/useClick context))
                          (FUI/useDismiss context
                                          ;; probably want opt-in/out for these
                                          #js {:escapeKey            true
                                               :outsidePointerDown   false
                                               :referencePointerDown false
                                               :bubbles              false})]
                         (remove nil?)
                         (into []))))]

    ($ :<>
       ($ :div
          (merge
            (js->clj (.getReferenceProps ixs (clj->js {:ref (.-reference floating-state)})))
            {:class (concat [ ;; seems like a reasonable class...
                             ;; can't be overwritten at the moment
                             "max-w-max"] (:class anchor-comp-props))}
            (dissoc anchor-comp-props :class))
          anchor-comp)

       ($ FUI/FloatingPortal
          (when open
            ($ FUI/FloatingFocusManager {:context context}
               ($ :div
                  (merge
                    (js->clj (.getFloatingProps
                               ixs (clj->js {:ref   (.-floating floating-state)
                                             :style {:position (.-strategy floating-state)
                                                     :top      (or (.-y floating-state) "")
                                                     :left     (or (.-x floating-state) "")
                                                     :maxWidth "calc(100vw - 10px)"
                                                     :overflow "scroll"
                                                     :zIndex   "50"}})))
                    popover-comp-props)
                  popover-comp)))))))
