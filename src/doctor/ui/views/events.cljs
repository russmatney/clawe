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


(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format "MMM dd h:mm a" (:event/timestamp it))))

(comment
  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes))))

(defn event-comp [{:keys [selected?
                          selected-ref]
                   :as   opts}
                  it]
  [:div
   {:class ["px-4" "pt-4" "pb-16" "flex-col"
            "hover:bg-yo-blue-600"
            (when selected? "bg-yo-blue-500")]
    :ref   selected-ref}
   (when-let [ts (:event/timestamp it)]
     [:div
      {:class ["text-2xl" "pb-4"]}
      (if-let [time-str (->date-str opts it)]
        time-str
        ts)])

   (when (:file/web-asset-path it)
     [:div
      {:class ["w-3/5"]}
      [components.screenshot/screenshot-comp opts it]])

   (when (:git.commit/hash it)
     [:div
      [components.git/commit-comp opts it]])

   (when (:org/name it)
     [:div
      ;; TODO non-todo version of org-item
      [components.todo/todo opts it]])

   [:div
    {:class ["flex" "pt-4"]}
    [components.debug/raw-metadata
     (-> opts
         (assoc :label "Event metadata")
         (assoc :initial-show? false)
         (assoc :exclude-key #{:git.commit/body
                               :git.commit/full-message})) it]]])

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
      [event-count-comp {:label "org-notes" :count org-note-count}]]])
  )

(defn event-date-popover [{:keys [date events]}]
  [:div
   {:class ["mt-4"
            "flex" "flex-col"
            "bg-city-blue-800"
            "p-4"
            "rounded-lg"
            "shadow-lg"]}

   [:span
    {:class ["text-center" "pb-2"]}
    (t/format "MMM d" date)]

   [event-count-list events]])


(defn events-for-dates [dates events]
  (let [on-at-least-one-date (->> dates (map t/date) (into #{}))]
    (->> events
         (remove (comp nil? :event/timestamp))
         (filter
           (comp on-at-least-one-date t/date :event/timestamp)))))

(defn event-page []
  (let [{:keys [items]}   (hooks.events/use-events)
        all-item-dates    (->> items
                               (map :event/timestamp)
                               (remove nil?)
                               (map t/date)
                               (into #{}))
        cursor-idx        (uix/state 0)
        selected-elem-ref (uix/ref)
        selected-dates    (uix/state #{})

        events               (cond->> items
                               (seq @selected-dates)
                               (events-for-dates @selected-dates))
        event-count          (count events)
        on-cursor-up-or-down (fn [_]
                               (when @selected-elem-ref
                                 (.scrollIntoView @selected-elem-ref)))]
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
       {:on-date-click  #(swap! selected-dates (fn [ds] (w/toggle ds %)))
        :date-has-data? all-item-dates
        :selected-dates @selected-dates
        :popover-comp   (fn [{:keys [date] :as opts}]
                          [event-date-popover
                           (assoc opts :events (events-for-dates #{date} items))])}
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

     (for [[i evt] (->> events (map-indexed vector))]
       (let [selected? (#{i} @cursor-idx)]
         ^{:key i}
         [event-comp
          {:selected?    selected?
           :selected-ref (when selected? selected-elem-ref)}
          evt]))]))
