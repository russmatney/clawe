(ns components.timeline
  (:require [tick.core :as t]))

(defn events-date-data
  ([ts] (events-date-data {} ts))
  ([{:keys [months-ago days-ago]} timestamps]
   (let [months-ago (or months-ago 8)
         days-ago   (or days-ago 20)
         timestamps (->> timestamps)
         newest     (some->> timestamps (sort-by t/>) first)
         oldest     (some->> timestamps (sort-by t/<) first)]
     (when newest
       {:timestamps    timestamps
        :newest        newest
        :oldest        oldest
        :newest-dates  (t/range
                         (-> newest (t/<< (t/new-period days-ago :days)))
                         (-> newest (t/>> (t/new-period 1 :days)))
                         (t/new-period 1 :days))
        :newest-months (t/range
                         (-> newest (t/<< (t/new-period months-ago :months)))
                         (-> newest (t/>> (t/new-period 1 :months)))
                         (t/new-period 1 :months))}))))

(comment
  (->>
    (t/zoned-date-time)
    list
    events-date-data)

  (->>
    (t/zoned-date-time)
    list
    events-date-data
    :newest-months
    (map t/zoned-date-time)
    (map (fn [date-month]
           (t/format
             (t/formatter "MMM")
             date-month)))))


(defn day-picker [opts events]
  (let [{:keys [on-date-click]} opts
        {:keys [newest
                newest-dates
                newest-months]}
        (->>
          events
          (map :event/timestamp)
          (remove nil?)
          (events-date-data {:months-ago 2}))]
    [:div
     {:class ["flex" "flex-col" "w-full"]}

     [:div
      {:class ["flex" "flex-row"
               "ml-auto"
               ]}
      (for [date-month newest-months]
        [:div
         {:key   (str date-month)
          :class ["flex" "flex-col"
                  "px-10" "py-3"
                  "justify-center"
                  "text-center"
                  "border-r" "border-l"
                  "border-city-green-400"
                  "border-opacity-30"]}
         (t/format
           (t/formatter "MMM")
           date-month)])]

     [:div
      {:class ["flex" "flex-row"
               "ml-auto"
               "border-t"
               "border-city-green-400"
               "border-opacity-30"]}
      (for [date newest-dates]
        [:div
         {:key      (str date)
          :class    ["flex" "flex-col"
                     "px-4" "py-6"
                     "justify-center"
                     "text-center"
                     "border-r" "border-l"
                     "border-city-green-400"
                     "border-opacity-30"
                     "hover:bg-yo-blue-400"
                     "cursor-pointer"]
          :on-click #(on-date-click date)}
         [:span
          (some->
            (t/format (t/formatter "E") date)
            first)]
         [:span (t/format (t/formatter "d") date)]])]]))
