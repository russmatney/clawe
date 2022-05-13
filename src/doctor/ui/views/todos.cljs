(ns doctor.ui.views.todos
  (:require
   [uix.core.alpha :as uix]
   [components.todo]
   [hooks.todos]
   [clojure.string :as string]
   [tick.core :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todo list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label selected on-select]} todos]
  [:div {:class ["flex" "flex-col"]}
   [:div {:class ["text-2xl" "p-2" "pt-4"]} label]
   [:div {:class ["flex" "flex-row" "flex-wrap" "justify-center"]}
    (for [[i it] (->> todos (map-indexed vector))]
      ^{:key i}
      [components.todo/todo
       {:on-select    #(on-select it)
        :is-selected? (= selected it)}
       (assoc it :index i)])]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouping and filtering
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-filter-defs
  {:file-name {:label            "Source File"
               :group-by         :todo/file-name
               :group-filters-by (fn [fname]
                                   (some-> fname (string/split #"/") first))
               :filter-options   [{:label    "All Dailies"
                                   :match-fn (fn [fname]
                                               (some-> fname (string/split #"/") first #{"daily"}))}
                                  {:label    "All Workspaces"
                                   :match-fn (fn [fname]
                                               (some-> fname (string/split #"/") first #{"workspaces"}))}]
               }
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
    ;; {:filter-key :scheduled
    ;;  :match      nil
    ;;  ;; :match-fn   (fn [d]
    ;;  ;;               (if-not d true
    ;;  ;;                       (and
    ;;  ;;                         (t/> d (t/yesterday))
    ;;  ;;                         (t/< d (t/tomorrow)))))
    ;;  }
    })

(defn split-counts [items {:keys [set-group-by toggle-filter-by
                                  items-group-by items-filter-by]}]
  [:div.flex.flex-row.flex-wrap
   (for [[i [filter-key filter-def]] (map-indexed vector all-filter-defs)]
     (let [split             (->> items
                                  (group-by (:group-by filter-def))
                                  (map (fn [[v xs]] [v (count xs)])))
           group-by-enabled? (= items-group-by filter-key)]
       ^{:key i}
       [:div
        {:class [(when-not (zero? i) "px-8")]}
        [:div.text-xl.font-nes
         {:class    ["cursor-pointer"
                     "hover:text-city-red-600"
                     (when group-by-enabled?
                       "text-city-pink-400")]
          :on-click #(set-group-by filter-key)}
         (:label filter-def)]

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
                 {:class ["font-nes" "ml-auto"]}
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
                   [:span.p-1.text-xl.w-10.text-center v]
                   (let [format-label (:format-label filter-def str)]
                     [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])]))])])]))])

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
      [split-counts items {:set-group-by    #(reset! items-group-by %)
                           :toggle-filter-by
                           (fn [f-by]
                             (swap! items-filter-by
                                    #(if (% f-by) (disj % f-by) (conj % f-by))))
                           :items-filter-by @items-filter-by
                           :items-group-by  @items-group-by}]]

     [todo-list
      {:label     "In Progress"
       :on-select (fn [it] (reset! selected it))
       :selected  @selected}
      (->> db-todos (filter (comp #{:status/in-progress} :todo/status)))]

     [todo-list
      {:label     "Incomplete DB Todos"
       :on-select (fn [it] (reset! selected it))
       :selected  @selected}
      (->> db-todos
           (remove (comp #{:status/done
                           :status/cancelled} :todo/status)))]

     (for [[i {:keys [item-group label]}]
           (map-indexed vector filtered-item-groups)]
       ^{:key i}
       [todo-list {:label     (str label " (" (count item-group) ")")
                   :on-select (fn [it] (reset! selected it))
                   :selected  @selected}
        item-group])]))
