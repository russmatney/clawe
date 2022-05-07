(ns components.timeline
  (:require [tick.core :as t]
            [wing.core :as w]))

(defn events-date-data
  ([ts] (events-date-data {} ts))
  ([{:keys [months-ago days-ago]} timestamps]
   (when (seq timestamps)
     (let [months-ago (or months-ago 8)
           days-ago   (or days-ago 20)
           newest     (some->> timestamps (sort t/>) first)
           oldest     (some->> timestamps (sort t/<) first)
           all-dates  (t/range oldest
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

(comment
  (def --dates
    [(t/zoned-date-time)
     (t/<< (t/zoned-date-time) (t/new-period 1 :months))
     (t/<< (t/zoned-date-time) (t/new-period 1 :days))])

  (events-date-data --dates)

  (->> --dates events-date-data :dates-by-month)

  (->> --dates
       events-date-data
       :all-dates
       (map t/zoned-date-time)
       (map (fn [date-month]
              (t/format (t/formatter "EE d") date-month))))

  (->> --dates
       events-date-data
       :all-months
       (map t/zoned-date-time)
       (map (fn [date-month]
              (t/format (t/formatter "MMM") date-month)))))

(defn day-picker [opts timestamps]
  (let [{:keys [on-date-click
                date-filter
                date-has-data?
                selected-date]} opts
        {:keys [newest dates-by-month]}
        (->> timestamps
             (events-date-data {:months-ago 2}))
        ;; dates-by-month
        ;; (->> dates-by-month
        ;;      (map (fn [[m ds]]
        ;;             [m (->> ds (filter (comp date-filter t/date)))]))
        ;;      (into {}))
        ]
    [:div
     {:class ["flex" "flex-col" "w-full"]}

     [:div
      {:class ["flex" "flex-row" "ml-auto"]}
      (for [[month dates] dates-by-month]
        [:div
         {:key   (str month)
          :class ["flex" "flex-col"
                  "px-10" "py-3"
                  "justify-center"
                  "text-center"]}
         [:div
          {:class
           ["py-3"
            "border-r" "border-l"
            "border-city-green-400"
            "border-opacity-30"]}
          (t/format (t/formatter "MMM") month)]

         [:div
          {:class ["flex" "flex-row"
                   "flex-wrap"
                   "ml-auto"
                   "border-t"
                   "border-city-green-400"
                   "border-opacity-30"]}
          (for [date dates]
            (let [is-selected? (and selected-date (t/= (t/date date) (t/date selected-date)))
                  has-data?    (date-has-data? (t/date date))]
              [:div
               {:key      (str date)
                :class    ["flex" "flex-col"
                           "px-4" "py-6"
                           "justify-center"
                           "text-center"
                           "border-r" "border-l"
                           "border-city-green-400"
                           "border-opacity-30"
                           (when is-selected? "bg-yo-blue-500")
                           (when-not has-data? "bg-city-blue-900")
                           (when has-data? "hover:bg-yo-blue-400")
                           (when has-data? "cursor-pointer")]
                :on-click #(on-date-click date)}
               [:span
                (some-> (t/format (t/formatter "E") date) first)]
               [:span (t/format (t/formatter "d") date)]]))]])]]))
