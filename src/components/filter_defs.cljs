(ns components.filter-defs
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [uix.core :as uix :refer [$]]

   [dates.tick :as dates.tick]
   [components.todo :as todo]
   [components.note :as note]
   ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouping and filtering
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn short-path-days-ago
  ([] (short-path-days-ago 0))
  ([n]
   (str
     "daily/"
     (t/format "YYYY-MM-dd" (t/<< (dates.tick/now)
                                  (t/new-duration n :days)))
     ".org")))

(comment
  (t/format "YYYY-MM-dd" (t/<< (dates.tick/now)
                               (t/new-duration 0 :days))))

(defn is-daily-fname [fname]
  (some-> fname (string/includes? "daily")))

(defn path->basename [fname]
  (let [[f s & _rest]
        (some-> fname (string/split #"/") reverse)]
    (->>
      [s f]
      (remove nil?)
      (map #(-> % (string/replace #".org" "") (->> (take 10) (apply str))))
      (string/join "/"))))

(def all-filter-defs
  {:filters/short-path {:label            "File"
                        :group-by         (fn [it]
                                            (or (:org/short-path it) (:org/source-file it)))
                        :group-filters-by (fn [fname]
                                            (some-> fname (string/split #"/") reverse (->> (drop 1) first)))
                        :group-by-label   (fn [label]
                                            (or
                                              (path->basename label)
                                              "No :org/short-path"))
                        :sort-groups-fn   (fn [item-groups]
                                            ;; TODO refactor to sort by last-modified date, most recent first
                                            (sort-by (comp count :item-group) > item-groups))
                        :filter-options   [{:label    "All Dailies"
                                            :match-fn is-daily-fname}]
                        :format-label     path->basename}
   :filters/tags       {:label          "Tags"
                        :group-by       :org/tags
                        :group-by-label (fn [label]
                                          (or label "Untagged"))
                        :sort-groups-fn (fn [item-groups]
                                          (sort-by (comp count :item-group) > item-groups))}
   :filters/status     {:label    "Status"
                        :group-by :org/status}
   :filters/priority   {:label               "Priority"
                        :group-by            :org/priority
                        :group-by-label      (fn [label] (or label "Unprioritized"))
                        :group-by-label-comp (fn [opts]
                                               ($ todo/priority-label (assoc opts :fallback (:label opts))))}
   :filters/scheduled  {:label        "Scheduled"
                        :group-by     :org/scheduled
                        :format-label (fn [d] (if d
                                                (if (string? d) d
                                                    (->> d dates.tick/add-tz
                                                         (t/format "MMM d, YYYY")))
                                                "Unscheduled"))}

   :filters/published {:label    "Published"
                       :group-by (fn [note]
                                   (if (:blog/published note)
                                     "Published" "Unpublished"))}


   :filters/event-timestamp
   {:label          "Event Timestamp"
    :group-by       (fn [it] (some-> it :event/timestamp dates.tick/parse-time-string t/date))
    :sort-groups-fn (fn [item-groups]
                      (->> item-groups (sort-by :label dates.tick/sort-latest-first)))
    :filter-options [{:label    "Today"
                      :match-fn (fn [lm] (t/>= lm (t/today)))}
                     {:label    "Yesterday"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 1 :days)))))}
                     {:label    "Last 3 days"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 4 :days)))))}
                     {:label    "Last 7 days"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 8 :days)))))}]}

   :filters/last-modified-date
   {:label          "Last Modified Date"
    :group-by       (fn [it] (some-> it :file/last-modified dates.tick/parse-time-string t/date))
    :sort-groups-fn (fn [item-groups]
                      (->> item-groups (sort-by :label dates.tick/sort-latest-first)))
    :filter-options [{:label    "Today"
                      :match-fn (fn [lm] (t/>= lm (t/today)))}
                     {:label    "Yesterday"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 1 :days)))))}
                     {:label    "Last 3 days"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 4 :days)))))}
                     {:label    "Last 7 days"
                      :match-fn (fn [lm] (t/>= lm
                                               (t/date (t/<< (dates.tick/now) (t/new-duration 8 :days)))))}]}

   :filters/link-count
   {:label    "Link Count"
    :group-by #(->> % components.note/->all-links count)}})

(def fg-config
  "The filter-grouper config."
  {:all-filter-defs all-filter-defs
   :presets
   {:all
    {:filters     #{}
     :group-by    :filters/last-modified-date
     :sort-groups :filters/last-modified-date}
    :events
    {:filters     #{}
     :group-by    :filters/event-timestamp
     :sort-groups :filters/event-timestamp}
    }})
