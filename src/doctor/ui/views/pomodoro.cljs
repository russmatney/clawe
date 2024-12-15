(ns doctor.ui.views.pomodoro
  (:require
   [uix.core :as uix :refer [$ defui]]
   [tick.core :as t]
   [taoensso.telemere :as log]

   [dates.tick :as dates]
   [doctor.ui.hooks.use-timer :refer [use-interval]]
   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.handlers :as handlers]
   [components.debug :as debug]
   [components.actions :as components.actions]
   ))

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

(defui bar [_opts]
  (let [{:keys [data]}         (hooks.use-db/use-query
                                 {:db->data (fn [db] (ui.db/pomodoro-state db))})
        p-state                data
        {:keys [current last]} p-state
        _time                  (use-interval {:->value  t/zoned-date-time
                                              ;; every 30s
                                              :interval 30000})]
    (log/log! {:data {:current (not (nil? current))
                      :last    (not (nil? last))}} "pomo bar rendering")
    ($ :div
       {:class ["flex flex-row" "items-center" "justify-between" "whitespace-nowrap"]}
       ($ :div
          {:class ["text-slate-200" "font-nes" "text-sm" "pr-2"]}
          (cond
            current
            ($ :span (dates/human-time-since (:pomodoro/started-at current)))
            last
            ($ :span
               "B: "
               (dates/human-time-since (:pomodoro/finished-at last)))))
       ($ components.actions/actions-list {:actions (handlers/pomodoro-actions p-state)}))))

(defui pomodoro-list [{:keys [pomodoros]}]
  (let [pairs (->> pomodoros
                   (partition 2 1))]
    ($ :div
       (for [[p prev] pairs]
         ($ :div
            {:key   (-> p :db/id str)
             :class ["flex flex-col"
                     "my-4" "pl-2"
                     "text-2xl" "whitespace-nowrap"]}
            ($ :span
               (str
                 "Started: "
                 (dates/human-time-since (:pomodoro/started-at p))

                 " "
                 (when (:pomodoro/finished-at p)
                   (str
                     "Finished: "
                     (dates/human-time-since (:pomodoro/finished-at p)))))

               "("
               (dates/human-time-since (:pomodoro/started-at p) (:pomodoro/finished-at p))
               ")")

            ($ :span
               (when prev
                 (str "Break: "
                      (dates/human-time-since (:pomodoro/finished-at prev) (:pomodoro/started-at p))))))))))

(defui widget [_opts]
  (let [_time
        (use-interval {:->value  t/zoned-date-time
                       :interval 30000})
        {:keys [data]}         (hooks.use-db/use-query
                                 {:db->data (fn [db] (ui.db/pomodoro-state db))})
        p-state                data
        {:keys [current last]} p-state

        {:keys [data]} (hooks.use-db/use-query
                         {:db->data (fn [db] (ui.db/pomodoros db))})
        pomodoros      data]
    (log/log! {:data {:current (not (nil? current))
                      :last    (not (nil? last))}} "pomo widget rendering")

    ($ :div
       {:class ["flex flex-col"]}
       ($ components.actions/actions-list {:actions (handlers/pomodoro-actions p-state)})
       ($ :div
          {:class ["flex flex-row" "items-center" "justify-around"
                   "bg-city-blue-700"
                   "text-city-green-200"
                   "font-mono"]}

          ($ debug/raw-metadata {:label "state" :data p-state})

          ;; TODO pull a component out of this
          ;; TODO genericize this thresholds thing
          (when current
            (let [{:keys [pomodoro/started-at]} current
                  active-thresholds
                  ;; TODO could make sense to have multiple active at a time
                  (->> current-thresholds
                       (filter (comp #(% started-at) :active?)))]
              ($ :div
                 {:class ["py-2" "px-4" "my-8"
                          "flex flex-col" "items-center"]}
                 ($ :span
                    {:class ["text-4xl"]}
                    "Current")
                 ($ :span
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
                      "!!"))
                 (when (->> active-thresholds (filter :label) seq)
                   (for [label (->> active-thresholds (map :label))]
                     ($ :span {:key label :class ["font-mono" "text-xl"]} label))))))

          (when last
            (let [{:keys [pomodoro/finished-at]} last
                  latest                         (:pomodoro/started-at current (dates/now))]
              ($ :div
                 {:class ["py-2" "px-4"]}
                 ($ :span
                    "Break: " (dates/human-time-since finished-at latest)))))

          (when last
            (let [{:pomodoro/keys [started-at finished-at]} last]
              ($ :div
                 {:class ["py-2" "px-4"]}
                 ($ :span
                    "Last: " (dates/human-time-since started-at finished-at))))))

       ($ pomodoro-list {:pomodoros pomodoros}))))
