(ns doctor.ui.views.pomodoro
  (:require
   [uix.core.alpha :as uix]
   [tick.core :as t]
   [dates.tick :as dates]
   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]
   [components.debug :as debug]
   [components.actions :as components.actions]))

(def current-thresholds
  [{:label   "Go get 'em!"
    :active? #(-> % dates/duration-since t/minutes (> 0))}
   {:label   "1st Half!"
    :active? #(-> % dates/duration-since t/minutes (> 5))}
   {:label   "2nd Half!"
    :active? #(-> % dates/duration-since t/minutes (> 20))}
   {:label     "4th Quarter!"
    :active?   #(-> % dates/duration-since t/minutes (> 30))
    :too-long? true}
   {:label     "Wrap it up!"
    :active?   #(-> % dates/duration-since t/minutes (> 40))
    :too-long? true}
   {:label     "Everybody take 5! Smoke if you got 'em!"
    :active?   #(-> % dates/duration-since t/minutes (> 60))
    :too-long? true}])

(defn bar [{:keys [conn]}]
  (let [{:keys [current last]} (ui.db/pomodoro-state conn)
        time                   (uix/state (t/zoned-date-time))
        interval               (atom nil)]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 500))
      (fn [] (js/clearInterval @interval)))
    [:div
     {:class ["flex flex-row" "items-center" "justify-around"]}

     [:div
      {:class ["text-slate-200" "font-nes" "mr-4"]}
      (cond current
            [:span
             "Pomodoro: " (dates/human-time-since (:pomodoro/started-at current))]
            last
            [:span
             "Break: "
             (dates/human-time-since (:pomodoro/finished-at last))])]

     [components.actions/actions-list (handlers/pomodoro-actions conn)]]))

(defn widget [{:keys [conn] :as _opts}]
  (let [time                               (uix/state (t/zoned-date-time))
        interval                           (atom nil)
        {:keys [current last] :as p-state} (ui.db/pomodoro-state conn)]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 500))
      (fn [] (js/clearInterval @interval)))

    [:div
     {:class ["flex flex-col"]}
     [components.actions/actions-list (handlers/pomodoro-actions conn)]
     [:div
      {:class ["flex flex-row" "items-center" "justify-around"
               "bg-city-blue-700"
               "text-city-green-200"
               "font-mono"]}

      [debug/raw-metadata {:label "state"} p-state]

      ;; TODO pull a component out of this
      ;; TODO genericize this thresholds thing
      (when current
        (let [{:keys [pomodoro/started-at]} current
              active-thresholds
              ;; TODO could make sense to have multiple active at a time
              (->> current-thresholds
                   (filter (comp #(% started-at) :active?)))]
          [:div
           {:class ["py-2" "px-4" "my-8"
                    "flex flex-col" "items-center"]}
           [:span
            {:class ["text-4xl"]}
            "Current"]
           [:span
            {:class (concat
                      ["font-nes" "my-4" "text-2xl"]
                      (->> active-thresholds
                           (mapcat :class))
                      (when (->> active-thresholds
                                 (filter :too-long?)
                                 seq)
                        ["text-city-pink-400" "font-nes" "font-bold"
                         "pl-2" "text-4xl" "whitespace-nowrap"]))}
            (dates/human-time-since started-at)
            (when (->> active-thresholds
                       (filter :too-long?)
                       seq)
              "!!")]
           (when (->> active-thresholds (filter :label) seq)
             (for [label (->> active-thresholds (map :label))]
               [:span {:class ["font-mono" "text-xl"]} label]))]))

      (when last
        (let [{:keys [pomodoro/finished-at]} last
              latest                         (:pomodoro/started-at current (dates/now))]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Break: " (dates/human-time-since finished-at latest)]]))

      (when last
        (let [{:pomodoro/keys [started-at finished-at]} last]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Last: " (dates/human-time-since started-at finished-at)]]))]]))
