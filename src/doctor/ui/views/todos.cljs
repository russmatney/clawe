(ns doctor.ui.views.todos
  (:require
   [uix.core.alpha :as uix]
   [components.todo]
   [components.floating :as floating]
   [hooks.todos]
   [clojure.string :as string]
   [tick.core :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouping and filtering
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-daily-fname [fname]
  (some-> fname (string/split #"/") first #{"daily"}))

(defn is-workspace-fname [fname]
  (some-> fname (string/split #"/") first #{"workspaces"}))

(def all-filter-defs
  {:file-name {:label            "File"
               :group-by         :todo/file-name
               :group-filters-by (fn [fname]
                                   (some-> fname (string/split #"/") first))
               :filter-options   [{:label    "All Dailies"
                                   :match-fn is-daily-fname}
                                  {:label    "All Workspaces"
                                   :match-fn is-workspace-fname}]
               :format-label     (fn [fname]
                                   (some-> fname (string/split #"/") second
                                           (string/replace #".org" "")
                                           (->>
                                             (take 10)
                                             (apply str))))}
   :status    {:label    "Status"
               :group-by :todo/status}
   :in-db?    {:label    "DB"
               :group-by (comp (fn [x] (if x :in-db :in-org)) :db/id)}
   :scheduled {:label        "Scheduled"
               :group-by     :org/scheduled
               :format-label (fn [d] (if d (t/format "MMM d, YYYY" d) "Unscheduled"))}})

(def default-filters
  #{{:filter-key :status :match :status/not-started}
    {:filter-key :status :match :status/in-progress}
    {:filter-key :file-name :match "todo/journal.org"}
    {:filter-key :file-name :match "todo/projects.org"}
    {:filter-key :file-name
     :match-fn   is-daily-fname
     :label      "All Dailies"}
    {:filter-key :file-name
     :match-fn   is-workspace-fname
     :label      "All Workspaces"}
    })

(defn filter-grouper [items {:keys [set-group-by toggle-filter-by
                                    items-group-by items-filter-by]}]
  [:div.flex.flex-row.flex-wrap
   {:class ["gap-x-3"]}
   (for [[i [filter-key filter-def]] (map-indexed vector all-filter-defs)]
     (let [split             (->> items
                                  (group-by (:group-by filter-def))
                                  (map (fn [[v xs]] [v (count xs)])))
           group-by-enabled? (= items-group-by filter-key)]
       [:div
        {:key i}

        [floating/popover
         {:hover true :click true
          :anchor-comp
          [:div.text-xl.font-nes
           {:class    ["cursor-pointer"
                       "hover:text-city-red-600"
                       (when group-by-enabled?
                         "text-city-pink-400")]
            :on-click #(set-group-by filter-key)}
           (:label filter-def)]
          :popover-comp
          [:div
           {:class ["bg-city-blue-800"
                    "text-city-pink-200"
                    "py-2" "px-4"]}

           ;; custom filter-opt selection
           (when (seq (:filter-options filter-def))
             [:div
              (for [[i filter-option] (->> filter-def :filter-options (map-indexed vector))]
                (let [filter-enabled? (items-filter-by (assoc filter-option :filter-key filter-key))]
                  [:div
                   {:key      i
                    :on-click #(toggle-filter-by (assoc filter-option :filter-key filter-key))
                    :class    ["flex" "flex-row" "font-mono"
                               "cursor-pointer"
                               "hover:text-city-red-600"
                               (when filter-enabled?
                                 "text-city-pink-400")]}
                   (:label filter-option)]))])

           (let [group-filters-by (:group-filters-by filter-def (fn [_] nil))]
             [:div
              {:class ["flex" "flex-row" "flex-wrap" "gap-x-4"]}
              (for [[i [group-label group]] (->> split (group-by (comp group-filters-by first)) (map-indexed vector))]
                [:div
                 {:key   i
                  :class ["flex" "flex-col"]}

                 (when group-label
                   [:div
                    {:class ["font-nes" "mx-auto"]}
                    group-label])

                 (for [[i [k v]] (->> group (sort-by second >) (map-indexed vector))]
                   (let [filter-enabled?
                         ;; TODO refactor to support 'all dailies' or 'all workspaces' as well
                         (items-filter-by {:filter-key filter-key :match k})]
                     [:div
                      {:key      i
                       :class    ["flex" "flex-row" "font-mono"
                                  "cursor-pointer"
                                  "hover:text-city-red-600"
                                  (when filter-enabled?
                                    "text-city-pink-400")]
                       :on-click #(toggle-filter-by {:filter-key filter-key :match k})}
                      (let [format-label (:format-label filter-def str)]
                        [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])
                      [:span.p-1.text-xl.w-10.text-center v]]))])])]}]]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; base widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget []
  (let [{:keys [items db-todos]} (hooks.todos/use-todos)

        selected        (uix/state (first items))
        items-group-by  (uix/state (some->> all-filter-defs first first))
        items-filter-by (uix/state default-filters)

        filtered-items
        (if-not (seq @items-filter-by) items
                (let [preds
                      (->> @items-filter-by
                           (group-by :filter-key)
                           (map (fn [[filter-key filter-defs]]
                                  (let [->value       (-> filter-key all-filter-defs :group-by)
                                        exact-matches (->> filter-defs (map :match) (into #{}))
                                        pred-matches  (->> filter-defs (map :match-fn) (remove nil?))
                                        is-match      (->> [exact-matches]
                                                           (filter seq)
                                                           (concat pred-matches)
                                                           ((fn [fns]
                                                              (apply some-fn fns))))]
                                    (comp is-match ->value)))))]
                  (->> items (filter (apply every-pred preds)))))

        filtered-item-groups (->> filtered-items
                                  (group-by (some-> @items-group-by all-filter-defs :group-by))
                                  (map (fn [[status its]]
                                         {:item-group its
                                          :label      status})))]

    [:div
     {:class ["flex" "flex-col" "flex-wrap"
              "overflow-hidden"
              "min-h-screen"
              "text-city-pink-200"]}

     [:div
      {:class ["p-4"]}
      [filter-grouper items {:set-group-by    #(reset! items-group-by %)
                             :toggle-filter-by
                             (fn [f-by]
                               (swap! items-filter-by
                                      #(if (% f-by) (disj % f-by) (conj % f-by))))
                             :items-filter-by @items-filter-by
                             :items-group-by  @items-group-by}]]

     [:div
      {:class ["flex" "flex-row" "flex-wrap"]}
      [components.todo/todo-list
       {:label     "In Progress"
        :on-select (fn [it] (reset! selected it))
        :selected  @selected}
       (->> db-todos (filter (comp #{:status/in-progress} :todo/status)))]

      [components.todo/todo-list
       {:label     "Incomplete DB Todos"
        :on-select (fn [it] (reset! selected it))
        :selected  @selected}
       (->> db-todos (remove (comp #{:status/done
                                     :status/cancelled} :todo/status)))]

      (for [[i {:keys [item-group label]}]
            (map-indexed vector filtered-item-groups)]
        ^{:key i}
        [components.todo/todo-list {:label     (str label " (" (count item-group) ")")
                                    :on-select (fn [it] (reset! selected it))
                                    :selected  @selected}
         item-group])]]))
