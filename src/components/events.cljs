(ns components.events
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [uix.core.alpha :as uix]
   [wing.core :as w]

   [components.chess :as components.chess]
   [components.git :as components.git]
   [components.screenshot :as components.screenshot]
   [components.timeline :as components.timeline]
   [components.garden :as components.garden]
   [components.floating :as floating]
   [dates.tick :as dates.tick]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; pure event helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn events-for-dates [dates events]
  (let [on-at-least-one-date (->> dates (map t/date) (into #{}))]
    (->> events
         (remove (comp nil? :event/timestamp))
         (filter
           (comp on-at-least-one-date t/date :event/timestamp)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic event counts by type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn event-counts [events]
  (let [count-by (fn [t] (->> events (filter (comp #{t} :doctor/type)) count))]
    {:screenshot-count (count-by :type/screenshot)
     :commit-count     (count-by :type/commit)
     :org-note-count   (count-by :type/garden)
     :chess-game-count (count-by :type/lichess-game)}))

(defn event-count-comp [{:keys [label count]}]
  (when (and count (> count 0))
    [:span
     {:class ["whitespace-nowrap"]}
     (str count " " label)]))

(defn event-count-list [events]
  (let
      [{:keys [screenshot-count
               commit-count
               org-note-count
               chess-game-count]}
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
      [event-count-comp {:label "org-notes" :count org-note-count}]]
     [:span
      {:class ["whitespace-nowrap"]}
      [event-count-comp {:label "chess games" :count chess-game-count}]]]))


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
       (map (comp :start first)))

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
  (let [screenshots  (->> events (filter (comp #{:type/screenshot} :doctor/type)))
        commits      (->> events (filter (comp #{:type/commit} :doctor/type)))
        chess-games  (->> events (filter (comp #{:type/lichess-game} :doctor/type)))
        garden-notes (->> events (filter (comp #{:type/garden} :doctor/type)))]
    [:div
     {:class ["flex" "flex-col"]}

     [components.chess/cluster opts chess-games]
     [components.screenshot/cluster opts screenshots]
     [components.git/commit-list opts commits]

     ;; weak for now, but it's getting surfaced!
     [floating/popover
      {:hover true :click true
       :anchor-comp
       [:div (str (count garden-notes) " garden notes")]
       :popover-comp
       [:div
        (for [note garden-notes]
          ^{:key [(:org/name note)]}
          [components.garden/garden-node note])]}]]))

(defn event-clusters
  [opts events]
  (let [{:keys [grouped-events]}
        (grouped-event-data opts events)]
    [:div
     (for [[i [bucket evts]] (->> grouped-events (map-indexed vector))]
       (let [{:keys [start end]} bucket
             start               (dates.tick/add-tz start)
             end                 (dates.tick/add-tz end)
             show-end?           (cond
                                   (t/= start end)                 false
                                   (t/< (t/between start end)
                                        (t/new-duration 1 :hours)) false
                                   :else                           true)]
         [:div
          {:key   (str start)
           :class [(when (odd? i) "bg-yo-blue-800") "p-4"]}

          [:span
           {:class ["text-xl"]}
           (str (t/format "EEEE d ha" start)
                (when show-end?
                  (str " - " (t/format "ha" end))))]

          [event-count-list evts]

          [event-cluster opts evts]

          #_[basic-event-list {} events]]))]))

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
;; Full cluster component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn events-cluster [_opts items]
  (let [items          (->> items (filter :event/timestamp))
        all-item-dates (->> items
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
      [components.events/event-clusters {} events]]]))
