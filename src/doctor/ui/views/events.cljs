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
   [uix.core.alpha :as uix]))


(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format
      (t/formatter "MMM dd h:mm a")
      (:event/timestamp it))))

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

(defn events-for-date [date events]
  (->> events
       (filter (fn [evt]
                 (let [evt-date (:event/timestamp evt)]
                   (t/=
                     (t/date evt-date)
                     (t/date date)))))))

(defn event-page []
  (let [{:keys [items]}   (hooks.events/use-events)
        all-item-dates    (->> items
                               (map :event/timestamp)
                               (remove nil?)
                               (map t/date)
                               (into #{}))
        cursor-idx        (uix/state 0)
        selected-elem-ref (uix/ref)
        selected-date     (uix/state nil)

        events               (cond->> items
                               @selected-date
                               (events-for-date @selected-date))
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
       {:on-date-click  #(reset! selected-date %)
        :date-has-data? all-item-dates
        :selected-date  @selected-date}
       (->> items (map :event/timestamp) (remove nil?))]]

     ;; TODO filter by type (screenshots, commits, org items)
     [:div [:h1 {:class ["pb-4" "text-xl"]} event-count " Events"
            (when @selected-date
              (str " on " (t/format
                            (t/formatter "E MMM d")
                            @selected-date)))]]

     (for [[i evt] (->> events (map-indexed vector))]
       (let [selected? (#{i} @cursor-idx)]
         ^{:key i}
         [event-comp
          {:selected?    selected?
           :selected-ref (when selected? selected-elem-ref)}
          evt]))]))
