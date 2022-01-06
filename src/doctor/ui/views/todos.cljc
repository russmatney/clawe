(ns doctor.ui.views.todos
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[doctor.api.todos :as d.todos]]
       :cljs [[uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]
              [tick.alpha.api :as t]
              [hiccup-icons.fa :as fa]
              [doctor.ui.components.todos :as todos]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todos data api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-todos-handler [] (d.todos/get-todos))
(defstream todos-stream [] d.todos/*todos-stream*)

#?(:cljs
   (defn use-todos []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-todos-handler) handle-resp)
       (with-stream [] (todos-stream) handle-resp)

       {:items     @items
        :db-todos  (->> @items
                        (filter :db/id))
        :org-todos (->> @items
                        (remove :db/id))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todo
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn todo
     [{:keys [on-select]} item]
     (let [{:db/keys   [id]
            :org/keys  [body urls]
            :todo/keys [status name file-name last-started-at]} item]
       [:div
        {:class    ["py-2" "px-4" "w-1/3"
                    "border" "border-city-blue-600"
                    "bg-yo-blue-700"
                    "text-white"]
         :on-click #(on-select)}

        [:div
         {:class ["flex" "justify-between"]}
         [:div
          {:class ["text-3xl"]}
          (case status
            :status/done        fa/check-circle
            :status/not-started fa/sticky-note
            :status/in-progress fa/pencil-alt-solid
            :status/cancelled   fa/ban-solid
            [:div "no status"])]

         [todos/action-list item]]

        [:span
         {:class ["text-xl"]}
         name]

        (when last-started-at
          [:div
           {:class ["font-mono"]}
           (t/instant (t/new-duration last-started-at :millis))])

        [:div
         {:class ["font-mono"]}
         file-name]

        (when id
          [:div
           {:class ["font-mono"]}
           (str "DB ID: " id)])

        (when (seq body)
          [:div
           {:class ["font-mono" "text-city-blue-400"
                    "flex" "flex-col" "p-2"
                    "bg-yo-blue-500"]}
           (for [[i line] (map-indexed vector body)]
             (let [{:keys [text]} line]
               (cond
                 (= "" text)
                 ^{:key i} [:span {:class ["py-1"]} " "]

                 :else
                 ^{:key i} [:span text])))])

        (when (seq urls)
          [:div
           {:class ["font-mono" "text-city-blue-400"
                    "flex" "flex-col" "pt-4" "p-2"]}
           (for [[i url] (map-indexed vector urls)]
             ^{:key i}
             [:a {:class ["py-1"
                          "cursor-pointer"
                          "hover:text-yo-blue-400"
                          ]
                  :href  url}
              url])])])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todo list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn todo-list [{:keys [label selected on-select]} todos]
     [:div {:class ["flex" "flex-col"]}
      [:div {:class ["text-2xl" "p-2" "pt-4"]} label]
      [:div {:class ["flex" "flex-row" "flex-wrap" "justify-center"]}
       (for [[i it] (->> todos (map-indexed vector))]
         ^{:key i}
         [todo
          {:on-select    #(on-select it)
           :is-selected? (= selected it)}
          (assoc it :index i)])]]))

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

#?(:cljs
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
                 [:span.p-1.text-xl (str k)]]))]]))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; base widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn widget []
     (let [{:keys [items db-todos]} (use-todos)

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
           item-group])])))
