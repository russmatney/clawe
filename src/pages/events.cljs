(ns pages.events
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
   [clojure.string :as string]
   [components.floating :as floating]))

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
    (let [cursor-hovered? (when cursor-idx (#{i} @cursor-idx))]
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
     {:class ["flex" "flex-col" "px-4"]}
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
;; event clusters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn collect-buckets
  "Requires passed items to be sorted ahead of time.

  Groups sequential items based on their relation with the current bucket.
  "
  [{:keys [items include-in-bucket? new-bucket merge-into-bucket]}]
  (->> items
       (reduce
         (fn [buckets next]
           (if-not (seq buckets)
             (list (new-bucket next))
             (let [[last-bucket & rest-buckets] buckets]
               (if (include-in-bucket? last-bucket next)
                 (cons (merge-into-bucket last-bucket next) rest-buckets)
                 (cons (new-bucket next) buckets)))))
         (list))))

(defn event->bucket [buckets event]
  (let [ts (:event/timestamp event)]
    (->> buckets
         (filter (fn [{:keys [start end]}] (and (t/>= ts start) (t/<= ts end))))
         first)))

(defn grouped-event-data [{:keys [idle-time]} events]
  (let [idle-time      (or idle-time
                           ;; TODO should be able to support days _and_ periods here
                           (t/new-duration 3 :hours))
        buckets        (collect-buckets
                         {:items (->> events (sort-by :event/timestamp t/<))
                          :new-bucket
                          (fn [item] {:start (:event/timestamp item)
                                      :end   (:event/timestamp item)})
                          :merge-into-bucket
                          (fn [current-bucket next]
                            (assoc current-bucket :end (:event/timestamp next)))
                          :include-in-bucket?
                          (fn [current-bucket next]
                            (t/< (t/between (:end current-bucket)
                                            (:event/timestamp next))
                                 idle-time))})
        grouped-events (->> events
                            (group-by (partial event->bucket buckets))
                            (sort-by (comp :start first) t/>))]
    {:all-events     events
     :buckets        buckets
     :grouped-events grouped-events}))

(comment
  (t/< (t/between (t/now)
                  (-> (t/now) (t/>> (t/new-duration 300 :minutes))))
       (t/new-duration 2 :hours))

  (->> [(t/now)
        (-> (t/now) (t/>> (t/new-duration 30 :minutes)))
        (-> (t/now) (t/>> (t/new-duration 3 :hours)))
        (-> (t/now) (t/<< (t/new-duration 30 :minutes)))
        (-> (t/now) (t/<< (t/new-duration 3 :hours)))]
       (map (fn [t] {:event/timestamp t}))
       (grouped-event-data {:idle-time (t/new-duration 2 :hours)})
       :grouped-events
       (map (comp :start first))
       )

  (defn new-range [next]
    (let [ts (:event/timestamp next)]
      {:start ts :end ts}))

  (defn replaces-end? [last-bucket next]
    (t/< (t/between (:end last-bucket) (:event/timestamp next))
         (t/new-duration 2 :hours)))

  (->> [(t/now)
        (-> (t/now) (t/>> (t/new-duration 30 :minutes)))
        (-> (t/now) (t/>> (t/new-duration 3 :hours)))
        (-> (t/now) (t/<< (t/new-duration 30 :minutes)))
        (-> (t/now) (t/<< (t/new-duration 3 :hours)))]
       (sort t/<)
       (map (fn [t] {:event/timestamp t}))
       (reduce
         (fn [buckets next]
           (if-not (seq buckets)
             (list (new-range next))
             (let [[last-bucket & rest-buckets] buckets]
               (if (replaces-end? (:end last-bucket) next)
                 (cons (assoc last-bucket :end (:event/timestamp next)) rest-buckets)
                 (cons (new-range next) buckets)))))
         (list)))


  (->> [6 8 12 4 0]
       (sort)
       (reduce
         (fn [buckets next]
           (if-not (seq buckets)
             (list {:start next :end next})
             (let [[last-bucket & rest-buckets] buckets]
               (if (< (- next (:end last-bucket)) 3)
                 (cons (assoc last-bucket :end next) rest-buckets)
                 (cons {:start next :end next} buckets)))))
         (list)))

  (cons 1 '(2 3)))


(defn event-cluster [opts events]
  [:div
   {:class ["flex" "flex-row" "flex-wrap"]}
   (for [[i event] (->> events
                        (sort-by :event/timestamp t/<)
                        (map-indexed vector))]
     [:div
      {:key   i
       :class ["m-2"]}

      [:div
       (when (:git.commit/hash event)
         [floating/popover
          {:click true :hover true
           :anchor-comp
           [:div
            [components.git/commit-thumbnail opts event]]
           :popover-comp
           [:div
            [components.git/commit-popover opts event]]}])

       (when (:file/web-asset-path event)
         [floating/popover
          {:click       true :hover true
           :anchor-comp [:div
                         {:class ["border-city-blue-400"
                                  "border-opacity-40"
                                  "border"
                                  "w-36"]}
                         [components.screenshot/thumbnail opts event]]
           :popover-comp
           [:div
            {:class ["w-2/3"
                     "shadow"
                     "shadow-city-blue-800"
                     "border"
                     "border-city-blue-800"]}
            [components.screenshot/img event]]}])]])])

(defn event-clusters
  [opts events]
  (let [{:keys [grouped-events]}
        (grouped-event-data opts events)]
    [:div
     (for [[i [bucket evts]] (->> grouped-events (map-indexed vector))]
       (let [{:keys [start end]} bucket
             show-end?           (cond
                                   (t/= start end)                 false
                                   (t/< (t/between start end)
                                        (t/new-duration 1 :hours)) false
                                   :else                           true)]
         [:div
          {:key   (str start)
           :class [(when (odd? i) "bg-yo-blue-800")
                   "p-4"]
           }

          (str (t/format "MMM d ha" start)
               (when show-end?
                 (str " - " (t/format "ha" end))))

          [event-count-list evts]

          [event-cluster opts evts]

          #_[basic-event-list {} events]]))]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [{:keys [items]} (hooks.events/use-events)
        items           (->> items (filter :event/timestamp))
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
                                 :events (events-for-dates #{date} items)}])}
       (->> items (map :event/timestamp) (remove nil?))]]

     ;; TODO filter by type (screenshots, commits, org items)
     [:div
      [:h1 {:class ["px-4" "pb-4" "text-xl"]}
       event-count " Events"
       (if (seq @selected-dates)
         (str " on "
              (->> @selected-dates
                   (map #(t/format "E MMM d" %))
                   (string/join ", ")))
         (str " in the last 14 days"))]

      [event-count-list events]]

     [event-clusters {} events]

     #_[basic-event-list {:cursor-idx      cursor-idx
                          :cursor-elem-ref cursor-elem-ref} events]]))
