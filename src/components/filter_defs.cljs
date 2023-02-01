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
     (t/format "YYYY-MM-DD" (t/<< (dates.tick/now)
                                  (t/new-duration n :days)))
     ".org")))

(defn is-daily-fname [fname]
  (some-> fname (string/includes? "daily")))

(defn is-workspace-fname [fname]
  (some-> fname (string/includes? "workspaces")))

(defn path->basename [fname]
  (some-> fname (string/split #"/") reverse first
          (string/replace #".org" "")
          (->> (take 10) (apply str))))

(def all-filter-defs
  {:short-path {:label            "File"
                :group-by         (fn [it]
                                    (or (:org/short-path it) (:org/source-file it)))
                :group-filters-by (fn [fname]
                                    (some-> fname (string/split #"/") reverse (->> (drop 1) first)))
                :filter-options   [{:label    "All Dailies"
                                    :match-fn is-daily-fname}
                                   {:label    "All Workspaces"
                                    :match-fn is-workspace-fname}]
                :format-label     path->basename}
   :tags       {:label    "Tags"
                :group-by :org/tags
                ;; TODO show untagged as well
                }
   :status     {:label    "Status"
                :group-by :org/status}
   :priority   {:label    "Priority"
                :group-by :org/priority}
   :scheduled  {:label        "Scheduled"
                :group-by     :org/scheduled
                :format-label (fn [d] (if d
                                        (if (string? d) d
                                            (->> d dates.tick/add-tz
                                                 (t/format "MMM d, YYYY")))
                                        "Unscheduled"))}})

(def fg-config
  "The filter-grouper config."
  {:all-filter-defs all-filter-defs
   :preset-filter-groups
   {:default
    {:filters
     #{{:filter-key :status :match :status/not-started}
       {:filter-key :status :match :status/in-progress}
       {:filter-key :short-path :match "todo/journal.org"}
       {:filter-key :short-path :match "todo/projects.org"}
       {:filter-key :short-path :match-fn is-daily-fname :label "All Dailies"}}
     :group-by :short-path}}})
