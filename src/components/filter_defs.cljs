(ns components.filter-defs
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [dates.tick :as dates.tick]))


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

(defn is-workspace-fname [fname]
  (some-> fname (string/includes? "workspaces")))

(defn path->basename [fname]
  (let [[f s & _rest]
        (some-> fname (string/split #"/") reverse)]
    (->>
      [s f] (map #(-> % (string/replace #".org" "")
                      (->> (take 10) (apply str))))
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
                                            :match-fn is-daily-fname}
                                           {:label    "All Workspaces"
                                            :match-fn is-workspace-fname}]
                        :format-label     path->basename}
   :filters/tags       {:label          "Tags"
                        :group-by       :org/tags
                        :group-by-label (fn [label]
                                          (or label "Untagged"))
                        :sort-groups-fn (fn [item-groups]
                                          (sort-by (comp count :item-group) > item-groups))}
   :filters/status     {:label    "Status"
                        :group-by :org/status}
   :filters/priority   {:label          "Priority"
                        :group-by       :org/priority
                        :group-by-label (fn [label]
                                          (or label "Unprioritized"))}
   :filters/scheduled  {:label        "Scheduled"
                        :group-by     :org/scheduled
                        :format-label (fn [d] (if d
                                                (if (string? d) d
                                                    (->> d dates.tick/add-tz
                                                         (t/format "MMM d, YYYY")))
                                                "Unscheduled"))}

   :filters/last-modified-date
   {:label          "Last Modified Date"
    :group-by       (fn [it]
                      ;; TODO safe date conversion
                      (:file/last-modified it))
    :filter-options [{:label    "Today"
                      :match-fn (fn [_]
                                  ;; safe today func
                                  )}
                     {:label    "Yesterday"
                      :match-fn (fn [_]
                                  ;; safe yesterday func
                                  )}
                     {:label    "Last 3 days"
                      :match-fn (fn [_]
                                  ;; safe yesterday func
                                  )}
                     {:label    "Last 7 days"
                      :match-fn (fn [_]
                                  ;; safe yesterday func
                                  )}
                     ]
    :format-label   (fn [t]
                      ;; format date
                      (str t))}})

(def fg-config
  "The filter-grouper config."
  {:all-filter-defs all-filter-defs
   :presets
   {:default
    {:filters
     #{{:filter-key :filters/status :match :status/not-started}
       {:filter-key :filters/status :match :status/in-progress}
       {:filter-key :filters/short-path :match "todo/journal.org"}
       {:filter-key :filters/short-path :match "todo/projects.org"}
       {:filter-key :filters/short-path :match-fn is-daily-fname :label "All Dailies"}}
     :group-by    :filters/short-path
     :sort-groups :filters/short-path}}})
