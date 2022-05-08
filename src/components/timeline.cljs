(ns components.timeline
  (:require
   [tick.core :as t]
   [wing.core :as w]
   [components.floating :as floating]))

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
;; day picker component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-picker [opts timestamps]
  (let [{:keys [on-date-click
                date-has-data?
                selected-dates
                popover-comp]} opts
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
                 [floating/popover
                  {:anchor-comp  [:button {:class ["w-3" "h-3" "rounded" "bg-city-pink-400"]}]
                   :popover-comp [popover-comp {:date date}]}])]))]])]]))
