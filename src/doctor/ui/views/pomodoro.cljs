(ns doctor.ui.views.pomodoro
  (:require
   [uix.core.alpha :as uix]
   [doctor.ui.pomodoros :as pomodoros]
   [tick.core :as t]
   [dates.tick :as dates]
   [components.actions :as components.actions]))

(comment
  (t/format
    (t/formatter "h:mma")
    (dates/add-tz
      (t/zoned-date-time))))

(defn widget [_opts]
  (let [time     (uix/state (t/zoned-date-time))
        interval (atom nil)]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 1000))
      (fn [] (js/clearInterval @interval)))

    [:div
     {:class ["flex flex-row" "items-center"
              "bg-city-blue-700"
              "text-city-green-200"]}
     [:div
      {:class ["text-2xl" "font-nes" "pl-4"]}
      (t/format
        (t/formatter "h:mma")
        (dates/add-tz @time))]

     (let [p-state                (pomodoros/get-state)
           {:keys [current last]} p-state]
       [:div
        {:class ["ml-auto" "flex" "flex-row"]}

        (when last
          (let [{:keys [started-at finished-at]} last]
            [:div
             {:class ["py-2" "px-4"]}
             [:span
              "Last: " (dates/human-time-since started-at finished-at)]]))

        (when last
          (let [{:keys [finished-at]} last
                latest                (:started-at current)]
            [:div
             {:class ["py-2" "px-4"]}
             [:span
              "Break: " (dates/human-time-since finished-at latest)]]))

        (when current
          (let [{:keys [started-at]} current
                minutes              (t/minutes (dates/duration-since started-at))
                too-long?            (> minutes 40)]
            [:div
             {:class ["py-2" "px-4"]}
             "Current: "
             [:span
              {:class (when too-long? ["text-city-pink-400" "font-nes" "font-bold"
                                       "pl-2" "text-lg" "whitespace-nowrap"])}
              (dates/human-time-since started-at)
              (when too-long? "!!")]]))

        ;; buttons
        [components.actions/actions-list
         {:actions (pomodoros/actions)}]])]))
