(ns doctor.ui.views.events
  (:require
   [tick.core :as t]
   [hooks.events]
   [components.screenshot]
   [components.todo]
   [components.debug]
   [components.git]
   [components.timeline]
   [keybind.core :as key]
   [uix.core.alpha :as uix]
   [wing.core :as w]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; pure event helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->date-str [_opts event]
  (when (:event/timestamp event)
    (t/format "MMM dd h:mm a" (:event/timestamp event))))

(comment
  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes))))

(defn events-for-dates [dates events]
  (let [on-at-least-one-date (->> dates (map t/date) (into #{}))]
    (->> events
         (remove (comp nil? :event/timestamp))
         (filter
           (comp on-at-least-one-date t/date :event/timestamp)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v1 event comps and list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn event-comp [{:keys [cursor-hovered?
                          cursor-elem-ref]
                   :as   opts}
                  event]
  [:div
   {:class ["px-4" "pt-4" "pb-16" "flex-col"
            "hover:bg-yo-blue-600"
            (when cursor-hovered? "bg-yo-blue-500")]
    :ref   cursor-elem-ref}
   (when-let [ts (:event/timestamp event)]
     [:div
      {:class ["text-2xl" "pb-4"]}
      (if-let [time-str (->date-str opts event)]
        time-str
        ts)])

   (when (:file/web-asset-path event)
     [:div
      {:class ["w-3/5"]}
      [components.screenshot/screenshot-comp opts event]])

   (when (:git.commit/hash event)
     [:div
      [components.git/commit-comp opts event]])

   (when (:org/name event)
     [:div
      ;; TODO non-todo version of org-item
      [components.todo/todo opts event]])

   [:div
    {:class ["flex" "pt-4"]}
    [components.debug/raw-metadata
     (-> opts
         (assoc :label "Event metadata")
         (assoc :initial-show? false)
         (assoc :exclude-key #{:git.commit/body
                               :git.commit/full-message})) event]]])

(defn basic-event-list
  [{:keys [cursor-idx cursor-elem-ref]} events]
  (for [[i evt] (->> events (map-indexed vector))]
    (let [cursor-hovered? (#{i} @cursor-idx)]
      ^{:key i}
      [event-comp
       {:cursor-hovered? cursor-hovered?
        :cursor-elem-ref (when cursor-hovered? cursor-elem-ref)}
       evt])))

(defn use-keyboard-cursor [{:keys [cursor-idx max-idx min-idx
                                   on-up on-down]}]
  (key/bind! "j" ::cursor-down
             (fn [_ev]
               (swap! cursor-idx (fn [v]
                                   (let [new-v (inc v)]
                                     (if (> new-v max-idx)
                                       max-idx
                                       new-v))))
               (on-up @cursor-idx)))
  (key/bind! "k" ::cursor-up
             (fn [_ev]
               (swap! cursor-idx
                      (fn [v]
                        (let [new-v (dec v)]
                          (if (< new-v min-idx)
                            min-idx new-v))))
               (on-down @cursor-idx)))
  nil)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic event counts by type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn event-counts [events]
  (let [count-by (fn [f] (->> events (filter f) count))]
    {:screenshot-count (count-by :screenshot/time-string)
     :commit-count     (count-by :git.commit/hash)
     :org-note-count   (count-by :org/title)}))

(defn event-count-comp [{:keys [label count]}]
  (when (and count (> count 0))
    [:span
     {:class ["whitespace-nowrap"]}
     (str count " " label)]))

(defn event-count-list [events]
  (let
      [{:keys [screenshot-count
               commit-count
               org-note-count]}
       (event-counts events)]
    [:div
     {:class ["flex" "flex-col"]}
     [:span
      {:class ["whitespace-nowrap"]}
      [event-count-comp {:label "screenshots" :count screenshot-count}]]
     [:span
      {:class ["whitespace-nowrap"]}
      [event-count-comp {:label "commits" :count commit-count}]]
     [:span
      {:class ["whitespace-nowrap"]}
      [event-count-comp {:label "org-notes" :count org-note-count}]]]))

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
    [event-count-list events]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn event-page []
  (let [{:keys [items]} (hooks.events/use-events)
        all-item-dates  (->> items
                             (map :event/timestamp)
                             (remove nil?)
                             (map t/date)
                             (into #{}))

        cursor-idx      (uix/state 0)
        cursor-elem-ref (uix/ref)
        selected-dates  (uix/state #{})

        events      (cond->> items
                      (seq @selected-dates)
                      (events-for-dates @selected-dates))
        event-count (count events)

        on-cursor-up-or-down (fn [_]
                               (when @cursor-elem-ref
                                 (.scrollIntoView @cursor-elem-ref)))]
    (use-keyboard-cursor {:cursor-idx cursor-idx
                          :max-idx    (dec event-count)
                          :min-idx    0
                          :on-up      on-cursor-up-or-down
                          :on-down    on-cursor-up-or-down})

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"
              "p-6"]}

     [:div
      {:class ["pb-8"]}
      [components.timeline/day-picker
       {:on-date-click       #(swap! selected-dates (fn [ds] (w/toggle ds %)))
        :date-has-data?      all-item-dates
        :selected-dates      @selected-dates
        :popover-anchor-comp [:button {:class ["w-3" "h-3" "rounded" "bg-city-pink-400"]}]
        :date->popover-comp  (fn [date]
                               [event-timeline-popover
                                {:date   date
                                 :events (events-for-dates #{date} items)}])}
       (->> items (map :event/timestamp) (remove nil?))]]

     ;; TODO filter by type (screenshots, commits, org items)
     [:div
      [:h1 {:class ["pb-4" "text-xl"]}
       event-count " Events"
       (if (seq @selected-dates)
         (str " on "
              (->> @selected-dates
                   (map #(t/format "E MMM d" %))
                   (string/join ", ")))
         (str " in the last 14 days"))]

      [event-count-list events]]

     [basic-event-list {:cursor-idx      cursor-idx
                        :cursor-elem-ref cursor-elem-ref} events]

     ]))
