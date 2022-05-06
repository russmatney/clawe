(ns doctor.ui.views.todos
  (:require
   [uix.core.alpha :as uix]
   [components.todo]
   [hooks.todos]))

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

(def filter-defs
  {:file-name {:label    "Source File"
               :group-by :todo/file-name}
   :status    {:label    "Status"
               :group-by :todo/status}
   :in-db?    {:label    "DB"
               :group-by (comp (fn [x] (if x :in-db :in-org)) :db/id)}})

(def default-filters
  #{{:def-k :status :res :status/not-started}
    {:def-k :status :res :status/in-progress}
    {:def-k :file-name :res "todo/journal.org"}
    {:def-k :file-name :res "todo/projects.org"}})

(defn split-counts [items {:keys [set-group-by toggle-filter-by
                                  items-group-by items-filter-by]}]
  [:div.flex.flex-row.flex-wrap
   (for [[i [def-k split-def]] (map-indexed vector filter-defs)]
     (let [split             (->> items
                                  (group-by (:group-by split-def))
                                  (map (fn [[v xs]] [v (count xs)])))
           group-by-enabled? (= items-group-by def-k)]
       ^{:key i}
       [:div
        {:class [(when-not (zero? i) "px-8")]}
        [:div.text-xl.font-nes
         {:class    ["cursor-pointer"
                     "hover:text-city-red-600"
                     (when group-by-enabled?
                       "text-city-pink-400")]
          :on-click #(set-group-by def-k)}
         (:label split-def)]
        [:div
         (for [[i [k v]] (->> split (sort-by second) (map-indexed vector))]
           (let [filter-enabled? (items-filter-by {:def-k def-k :res k})]
             ^{:key i}
             [:div
              {:class    ["flex" "font-mono"
                          "cursor-pointer"
                          "hover:text-city-red-600"
                          (when filter-enabled?
                            "text-city-pink-400")]
               :on-click #(toggle-filter-by {:def-k def-k :res k})}
              [:span.p-1.text-xl v]
              [:span.p-1.text-xl (str k)]]))]]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; base widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget []
  (let [{:keys [items db-todos]} (hooks.todos/use-todos)

        selected        (uix/state (first items))
        items-group-by  (uix/state (some->> filter-defs first first))
        items-filter-by (uix/state default-filters)

        filtered-items
        (if-not (seq @items-filter-by) items
                (let [preds
                      (->> @items-filter-by
                           (group-by :def-k)
                           (map (fn [[def-k vals]]
                                  (comp
                                    (->> vals (map :res) (into #{}))
                                    (-> def-k filter-defs :group-by)))))]
                  (->> items (filter (apply every-pred preds)))))

        filtered-item-groups (->> filtered-items
                                  (group-by (some-> @items-group-by filter-defs :group-by))
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
