(ns pages.todos
  (:require
   [uix.core.alpha :as uix]
   [components.todo :as components.todo]
   [components.filter :as components.filter]
   [clojure.string :as string]
   [tick.core :as t]
   [doctor.ui.db :as ui.db]
   [dates.tick :as dates.tick]

   [doctor.ui.views.ingest :as ingest]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouping and filtering
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(def filter-grouper-config
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn]}]
  (let [todos    (ui.db/garden-todos conn {:n 1000})
        selected (uix/state (first todos))
        {:keys [filtered-item-groups filtered-items filter-grouper]}
        (components.filter/use-filter
          (assoc filter-grouper-config :items todos))]
    [:div
     {:class ["grid" "grid-flow-row" "place-items-center"
              "overflow-hidden"
              "min-h-screen"
              "text-city-pink-200"]}

     [ingest/ingest-buttons]

     [:div {:class ["p-4"]} filter-grouper]

     [:div
      {:class ["grid" "grid-flow-row"]}

      [components.todo/todo-list
       {:label     "In Progress"
        :on-select (fn [it] (reset! selected it))
        :selected  @selected}
       (->> #_todos
            filtered-items
            (filter
              (fn [todo]
                (or
                  (-> todo :org/status #{:status/in-progress})
                  (and
                    (-> todo :todo/queued-at)
                    (not
                      (-> todo :org/status #{:status/cancelled
                                             :status/done})))))))]

      (for [[i {:keys [item-group label]}]
            (->> filtered-item-groups
                 (sort-by :label >)
                 (map-indexed vector))]
        ^{:key i}
        [components.todo/todo-list {:label     (str label " (" (count item-group) ")")
                                    :on-select (fn [it] (reset! selected it))
                                    :selected  @selected
                                    :n         9}
         item-group])]]))
