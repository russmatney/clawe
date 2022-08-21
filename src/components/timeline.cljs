(ns components.timeline
  (:require
   [tick.core :as t]
   [wing.core :as w]
   [components.floating :as floating]
   [dates.tick :as dates.tick]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc date helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn timestamps-date-data
  ([ts] (timestamps-date-data {} ts))
  ([{:keys [days-ago]} timestamps]
   (when (seq timestamps)
     (let [days-ago        (or days-ago 13)
           newest          (some->> timestamps (sort t/>) first dates.tick/add-tz)
           oldest          (some->> timestamps (sort t/<) first dates.tick/add-tz)
           oldest-days-ago (t/<< newest (t/new-period days-ago :days))
           oldest          (if (t/> oldest oldest-days-ago) oldest oldest-days-ago)
           all-dates       (t/range oldest
                                    (t/>> newest (t/new-period 1 :days))
                                    (t/new-period 1 :days))
           dates-by-month
           (->> all-dates
                (map dates.tick/add-tz)
                (w/group-by t/month))]
       (when newest
         {:timestamps     timestamps
          :newest         newest
          :oldest         oldest
          :all-dates      all-dates
          :all-months     (t/range oldest newest (t/new-period 1 :months))
          :dates-by-month dates-by-month})))))

(comment
  ;; errors b/c insts need tzs to do durations
  (timestamps-date-data
    {:months-ago 2}
    [(t/inst)])

  (timestamps-date-data
    {:months-ago 2}
    [(dates.tick/now)]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; day picker component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-picker [opts timestamps]
  (let [{:keys [on-date-click
                date-has-data?
                selected-dates
                popover-anchor-comp
                date->popover-comp]} opts
        {:keys [dates-by-month]}
        (->> timestamps
             (timestamps-date-data {:months-ago 2}))]
    [:div
     {:class ["flex" "flex-col" "w-full"]}

     [:div
      {:class ["flex" "flex-row" "m-auto"]}
      (for [[month dates] dates-by-month]
        [:div
         {:key   (str month)
          :class ["flex" "flex-col"
                  "py-3"
                  "justify-center"
                  "text-center"]}
         [:div
          {:class ["py-3"
                   "border"
                   "border-city-green-400"
                   "border-opacity-30"]}
          (t/format "MMM" month)]

         [:div
          {:class ["flex" "flex-row"]}
          (for [[_idx date] (->> dates (map-indexed vector))]
            (let [is-selected? (and (seq selected-dates) (selected-dates date))
                  has-data?    (date-has-data? (t/date date))]
              [:div
               {:key (str date)
                :class
                ["flex" "flex-col"
                 "w-20"
                 "px-3" "py-2"
                 "justify-center"
                 "items-center"
                 "text-center"
                 "border-x"
                 (when-not is-selected? "border-b")
                 "border-city-green-400/30"
                 ;; "border-opacity-30"
                 (when is-selected? "bg-yo-blue-500")]}
               [:div
                {:class ["py-2"]}
                (some-> (t/format "E" date) first)]
               [:div
                {:class ["py-2"
                         "text-mono"]}
                [floating/popover
                 {:click true :hover true
                  :anchor-comp
                  [:div
                   {:class
                    ["flex"
                     "items-center"
                     "justify-center"
                     "text-center"
                     "w-10"
                     "h-10"
                     "rounded-full"
                     (cond
                       is-selected? "bg-city-blue-700"
                       has-data?    "bg-city-blue-900"
                       :else        "bg-yo-blue-900")
                     (when has-data? "hover:bg-yo-blue-400")
                     (when has-data? "cursor-pointer")]
                    :on-click #(on-date-click date)}
                   (t/format "d" date)]
                  :popover-comp
                  (when has-data? (date->popover-comp date))}]]

               [:div.flex-1
                {:class ["py-2"]}
                (when has-data?
                  [floating/popover
                   {:click        true :hover true
                    :anchor-comp  popover-anchor-comp
                    :popover-comp (date->popover-comp date)}])]]))]])]]))
