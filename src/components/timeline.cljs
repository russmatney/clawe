(ns components.timeline
  (:require
   [tick.core :as t]
   [wing.core :as w]
   ["@headlessui/react" :as Headless]
   [uix.core.alpha :as uix]

   ["@floating-ui/react-dom-interactions" :as FUI]
   ))

;; import {useFloating} from '@floating-ui/react-dom-interactions';

(comment


                                        ;
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc date helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn timestamps-date-data
  ([ts] (timestamps-date-data {} ts))
  ([{:keys [days-ago]} timestamps]
   (when (seq timestamps)
     (let [days-ago        (or days-ago 13)
           newest          (some->> timestamps (sort t/>) first)
           oldest          (some->> timestamps (sort t/<) first)
           oldest-days-ago (t/<< newest (t/new-period days-ago :days))
           oldest          (if (t/> oldest oldest-days-ago) oldest oldest-days-ago)
           all-dates       (t/range oldest
                                    (t/>> newest (t/new-period 1 :days))
                                    (t/new-period 1 :days))
           dates-by-month
           (->> all-dates (w/group-by t/month))]
       (when newest
         {:timestamps     timestamps
          :newest         newest
          :oldest         oldest
          :all-dates      all-dates
          :all-months     (t/range oldest newest (t/new-period 1 :months))
          :dates-by-month dates-by-month})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-popover [opts date]
  (let [{:keys [_open-popover?
                popover-comp
                ]} opts
        popover-comp
        (or popover-comp (fn [_] [:div date]))
        open?      (uix/state false)

        floating-state
        (FUI/useFloating {:open           @open?
                          :on-open-change #(do (reset! open? %)
                                               (println "floating swapped open" @open?))})

        floating  (. floating-state -floating)
        strategy  (. floating-state -strategy)
        x         (. floating-state -x)
        y         (. floating-state -y)
        reference (. floating-state -reference)
        context   (. floating-state -context)

        ixs
        (FUI/useInteractions
          #js [(FUI/useHover context)
               (FUI/useClick context)])

        ref-props   (. ixs getReferenceProps
                       (clj->js {:ref reference}))
        float-props (. ixs getFloatingProps
                       (clj->js {:ref   floating
                                 :style {:position strategy
                                         :top      (or y "")
                                         :left     (or x "")}}))]

    (def --ref-props ref-props)
    (def --float-props float-props)

    (merge (js->clj --ref-props)
           {
            ;; :on-mouse-enter (fn [_] (reset! open? true))
            ;; :on-mouse-leave (fn [_] (reset! open? false))
            :class ["w-3" "h-3" "rounded"
                    "bg-city-pink-400"]}
           )

    [:div
     [:button
      (merge (js->clj ref-props)
             {
              :on-mouse-enter (fn [_] (reset! open? true))
              :on-mouse-leave (fn [_] (reset! open? false))
              :class          ["w-3" "h-3" "rounded" "bg-city-pink-400"]
              ;; :on-click #(reset! open? true)
              })]

     [:> FUI/FloatingPortal
      (when @open?
        [:> FUI/FloatingFocusManager {:context context}
         [:div (js->clj float-props)
          [popover-comp {:date date}]]])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; day picker component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-picker [opts timestamps]
  (let [{:keys [on-date-click
                date-has-data?
                selected-dates]} opts
        {:keys [dates-by-month]}
        (->> timestamps
             (timestamps-date-data {:months-ago 2}))]
    [:div
     {:class ["flex" "flex-col" "w-full"]}

     [:div
      {:class ["flex" "flex-row" "ml-auto"]}
      (for [[month dates] dates-by-month]
        [:div
         {:key   (str month)
          :class ["flex" "flex-col"
                  "py-3"
                  "justify-center"
                  "text-center"]}
         [:div
          {:class ["py-3"]}
          (t/format "MMM" month)]

         [:div
          {:class ["flex" "flex-row"
                   "border-t"
                   "border-city-green-400"
                   "border-opacity-30"]}
          (for [[_idx date] (->> dates (map-indexed vector))]
            (let [is-selected? (and (seq selected-dates) (selected-dates date))
                  has-data?    (date-has-data? (t/date date))]
              [:div
               {:key      (str date)
                :class
                ["flex" "flex-col"
                 "w-14"
                 "px-4" "py-6"
                 "justify-center"
                 "text-center"
                 "border-r"
                 "border-city-green-400"
                 "border-opacity-30"
                 (when is-selected? "bg-yo-blue-500")
                 (when-not has-data? "bg-city-blue-900")
                 (when has-data? "hover:bg-yo-blue-400")
                 (when has-data? "cursor-pointer")]
                :on-click #(on-date-click date)}
               [:span
                (some-> (t/format "E" date) first)]
               [:span (t/format "d" date)]

               (when has-data?
                 [day-popover
                  (-> opts
                      ;; useful for debugging (renders the popover for the first date)
                      ;; (assoc :open-popover? (zero? idx))
                      )
                  date])]))]])]]))
