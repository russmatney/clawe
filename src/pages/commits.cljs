(ns pages.commits
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [uix.core.alpha :as uix]
   [wing.core :as w]

   [hooks.commits]
   [hooks.repos]
   [components.debug]
   [components.chess]
   [components.git]
   [components.screenshot]
   [components.timeline]
   [components.todo]
   [components.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event-timeline-popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn event-timeline-popover [{:keys [date events]}]
  [:div
   {:class ["mt-4"
            "flex" "flex-col"
            "bg-city-blue-800"
            "p-2"
            "pt-4"
            "rounded-lg"
            "shadow-lg"
            "bg-opacity-80"
            "border"
            "border-city-green-300"
            "text-city-black-100"]}

   [:span
    {:class ["text-center" "pb-2"]}
    (t/format "MMM d" date)]

   [:div
    {:class ["bg-city-red-900" "rounded-lg" "px-4" "p-2"]}
    [components.events/event-count-list events]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [repo-resp       (hooks.repos/use-repos)
        repos           (:items repo-resp)
        {:keys [items]} (hooks.commits/use-commits)
        items           (->> items (filter :event/timestamp))
        all-item-dates  (->> items
                             (map :event/timestamp)
                             (remove nil?)
                             (map t/date)
                             (into #{}))

        selected-dates (uix/state #{})

        events      (cond->> items
                      (seq @selected-dates)
                      (components.events/events-for-dates @selected-dates))
        event-count (count events)]

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"
              "py-6"]}

     [:div
      {:class ["pb-8" "px-6"]}
      [components.timeline/day-picker
       {:on-date-click       #(swap! selected-dates (fn [ds] (w/toggle ds %)))
        :date-has-data?      all-item-dates
        :selected-dates      @selected-dates
        :popover-anchor-comp [:button {:class ["w-3" "h-3" "rounded" "bg-city-pink-400"]}]
        :date->popover-comp  (fn [date]
                               [event-timeline-popover
                                {:date   date
                                 :events (components.events/events-for-dates #{date} items)}])}
       (->> items (map :event/timestamp) (remove nil?))]]

     [:div
      [:h1 {:class ["px-4" "text-xl"]}
       event-count " Events"
       (if (seq @selected-dates)
         (str " on "
              (->> @selected-dates
                   (map #(t/format "E MMM d" %))
                   (string/join ", ")))
         (str " in the last 14 days"))]

      [:div
       {:class ["px-4"]}
       [components.events/event-count-list events]]]

     [:div
      {:class ["pt-2"]}
      [components.events/event-clusters {} events]]

     [:div
      {:class ["flex" "flex-col" "text-white"]}
      (for [repo repos]
        ^{:key (:repo/path repo)}
        [:div (:repo/path repo)])]]))
