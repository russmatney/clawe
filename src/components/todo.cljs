(ns components.todo
  (:require
   [tick.core :as t]
   [components.colors :as colors]
   [components.note :as note]
   [components.floating :as floating]
   [components.garden :as components.garden]
   [components.debug :as components.debug]
   [components.actions :as components.actions]
   [components.item :as item]
   [doctor.ui.handlers :as handlers]
   [dates.tick :as dates.tick]
   [uix.core.alpha :as uix]
   [util :as util]
   [clojure.set :as set]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; preds

(defn completed? [it]
  (-> it :org/status #{:status/done}))

(defn not-started? [it]
  (-> it :org/status #{:status/not-started}))

(defn skipped? [it]
  (-> it :org/status #{:status/skipped}))

(defn in-progress? [it]
  (-> it :org/status #{:status/in-progress}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sort todos

(defn sort-todos
  "Sorts todos according to status and priority"
  [its]
  (->> its
       (sort-by
         (fn [it]
           ;; TODO include :todo/queued-at
           (cond->
               0
             ;; move finished to back
             (or (completed? it)
                 (skipped? it)) (+ 1000)

             ;; sort by priority
             (:org/priority it)
             (+ (util/label->comparable-int (:org/priority it)))

             (not (:org/priority it))
             (+ 100)

             ;; move in-progress to front
             (in-progress? it) (- 100)))
         ;; lower number means earlier in the order
         <)
       (sort-by :todo/queued-at dates.tick/sort-latest-first)))

(defn queued [todo]
  (let [queued-at (:todo/queued-at todo)]
    (when queued-at
      [:div
       {:class ["text-slate-400" "font-mono"]}
       (str "queued: "
            (t/format "E h:mma" (dates.tick/add-tz (t/instant queued-at))))])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; priority-label

(defn priority-label
  ([it] (priority-label nil it))
  ([opts it]
   (let [priort (cond (string? it) it
                      (map? it)    (:org/priority it)
                      :else        it)]
     (when priort
       [:span
        (merge {:class
                (concat
                  ["whitespace-nowrap" "font-nes"
                   "cursor-pointer"
                   "hover:line-through"]
                  (cond
                    (and (map? it)
                         (completed? it)) ["text-city-blue-dark-400"]
                    (and (map? it)
                         (skipped? it))   ["text-city-blue-dark-400"]
                    (#{"A"} priort)       ["text-city-red-400"]
                    (#{"B"} priort)       ["text-city-pink-400"]
                    (#{"C"} priort)       ["text-city-green-400"]))
                :on-click #(handlers/remove-priority
                             (dissoc it :actions/inferred))}
               opts)
        (str "#" priort " ")]))))

(defn priority-setter-actions
  ([it] (priority-setter-actions nil it))
  ([_opts it]
   (concat
     (->> ["A" "B" "C"]
          (map (fn [p]
                 {:action/label    (str "#" p)
                  :action/on-click (fn [_]
                                     (-> it
                                         (dissoc :actions/inferred)
                                         (handlers/set-priority p)))})))
     [{:action/label "Clear"
       :action/on-click
       (fn [_] (handlers/remove-priority (dissoc it :actions/inferred)))}])))

(defn priority-setters
  ([it] (priority-setters nil it))
  ([opts it]
   (components.actions/actions-list
     (priority-setter-actions opts it))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; level

(defn level [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    [components.debug/raw-metadata
     {:label
      [:div
       {:class ["whitespace-nowrap" "font-nes"]}
       (->> (repeat level "*") (apply str))]}
     it]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; status

(defn status-plain [it]
  (when (:org/status it)
    [:span
     {:class ["ml-2" "whitespace-nowrap" "font-nes"
              "cursor-pointer"
              "text-slate-300"
              "hover:opacity-50"]}
     (cond
       (completed? it)   "[X]"
       (skipped? it)     "SKIP"
       (in-progress? it) "[-]"
       (not-started? it) "[ ]")]))

(defn status
  ([it] [status nil it])
  ([_opts it]
   (when (:org/status it)
     (let [hovering? (uix/state nil)]
       [:span
        {:class          ["ml-2" "whitespace-nowrap" "font-nes"
                          "cursor-pointer"
                          "text-slate-300"
                          "hover:opacity-50"]
         :on-mouse-enter (fn [_] (reset! hovering? true))
         :on-mouse-leave (fn [_] (reset! hovering? false))
         :on-click
         (fn [_]
           ;; TODO refactor this logic into...something?
           ;; needs to support going from one state to another... maybe via a popup menu, with a default
           (handlers/todo-set-new-status
             (dissoc it :actions/inferred)
             (cond
               (completed? it)   :status/not-started
               (skipped? it)     :status/not-started
               (in-progress? it) :status/done
               (not-started? it) :status/in-progress)))}
        (cond
          (and (completed? it) (not @hovering?))   "[X]"
          (and (completed? it) @hovering?)         "[ ]"
          (and (skipped? it) (not @hovering?))     "SKIP"
          (and (skipped? it) @hovering?)           "[ ]"
          (and (in-progress? it) (not @hovering?)) "[-]"
          (and (in-progress? it) @hovering?)       "[X]"
          (and (not-started? it) (not @hovering?)) "[ ]"
          (and (not-started? it) @hovering?)       "[-]")]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags-list

(defn tags-list
  ([it] [tags-list nil it])
  ([_opts it]
   (when (seq (:org/tags it))
     [:span
      {:class ["text-md" "font-mono"
               "flex" "flex-row" "flex-wrap"]}
      ":"
      (for [[i tag] (->> it :org/tags (map-indexed vector))]
        ^{:key tag}
        [:span
         [:span
          {:class
           (concat ["cursor-pointer" "hover:line-through"]
                   (colors/color-wheel-classes {:type :line :i (+ 2 i)}))
           :on-click (fn [tag] (handlers/remove-tag
                                 (dissoc it :actions/inferred)
                                 tag))}
          tag]
         ":"])])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; content popover

(defn content [item]
  [floating/popover
   {:hover       true :click true
    :anchor-comp [:span "content"]
    :popover-comp

    [:div
     {:class ["grid" "grid-flow-col"]}
     ;; TODO show just this item's content
     #_[components.garden/org-body item]
     [components.garden/full-note item]]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; card

(defn card
  ([it] (card nil it))
  ([{:keys [hide-parent-names?]} it]
   [:div
    {:class
     (concat
       ["flex" "flex-col"
        "py-2" "px-3"
        "m-1"
        "rounded-lg"
        "w-96"
        "text-lg"
        "bg-yo-blue-700"]
       (cond
         (completed? it) ["text-city-blue-dark-400"]
         (skipped? it)   ["text-city-blue-dark-600"]
         ;; (not-started? it) []
         :else           ["text-city-blue-dark-200"]))}

    ;; top meta
    [:div
     {:class ["flex" "flex-row" "w-full" "items-center"]}

     [level it]
     [status it]
     [item/db-id it]
     [item/id-hash it]
     [:div {:class ["ml-auto"]}
      [priority-label it]]]

    ;; middle content
    [:div
     {:class ["flex" "flex-col" "pb-2"]}

     [tags-list it]

     [queued it]

     ;; name
     [:span
      [:span
       {:class (when (or (completed? it) (skipped? it)) ["line-through"])}
       (:org/name-string it)]]

     ;; time ago
     (when (and (completed? it) (:org/closed it))
       [:span
        {:class ["font-mono"]}
        (str
          (some-> it :org/closed
                  dates.tick/parse-time-string
                  dates.tick/human-time-since)
          " ago")])

     (when-not hide-parent-names?
       [item/parent-names {:n 2} it])]

    ;; bottom meta
    [:div
     {:class ["flex" "flex-row"
              "items-center"
              "text-sm"
              "mt-auto"
              "pb-2"]}

     [content it]

     ;; actions list
     [:span
      {:class ["ml-auto" "pl-4"]}
      [components.actions/actions-list
       {:actions       (handlers/->actions it)
        :hide-disabled true}]]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo cards

(defn card-or-card-group
  "Renders the passed todo as a card.
  If it has children (i.e. sub-tasks) they will be rendered as a group of cards."
  ([item] [card-or-card-group nil item])
  ([{:keys [filter-by filter-fn]} item]
   (let [todos             (->> item
                                (tree-seq (comp seq :org/items) :org/items)
                                (remove nil?)
                                (remove #(#{item} %))
                                (filter :org/status)
                                seq)
         todos             (cond->> todos
                             filter-by (filter filter-by)
                             filter-fn filter-fn)
         [children? todos] (if (seq todos) [true todos] [false [item]])
         groups            (group-by (fn [it] (-> it :org/parent-names str)) todos)]

     [:div {:class ["flex" "flex-col"]}
      (for [[pnames grouped-todos] groups]
        ^{:key (str pnames)}
        [:div {:class (concat ["flex" "flex-col"]
                              (when children?
                                ["border-city-green-400" "border"
                                 "bg-city-blue-900"
                                 "py-3"]))}

         (when children?
           [:div
            {:class ["flex" "flex-col" "p-2"]}
            [:div
             {:class ["flex" "flex-row" "items-center"]}

             [status item]
             [item/db-id item]
             [item/id-hash item]
             [:div {:class ["ml-auto"]}
              [tags-list item]]
             [priority-label item]]

            [:div
             {:class ["flex" "flex-row" "items-center" "px-3"]}
             [item/parent-names {:header? true} (first grouped-todos)]

             [:span
              {:class ["ml-auto"]}
              [components.actions/actions-list
               {:actions       (handlers/->actions item)
                :hide-disabled true}]]]])

         (if children?
           [:div
            {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}
            (for [[i td] (->> grouped-todos sort-todos (map-indexed vector))]
              ^{:key i}
              [:div
               {:class ["p-2"]}
               [card {:hide-parent-names? true} td]])]
           [:div
            {:class ["p-2"]}
            [card {:hide-parent-names? false} item]])])])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-row
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-row
  ([todo] [todo-row nil todo])
  ([_opts todo]
   [:div
    {:class ["grid" "grid-flow-col" "grid-cols-8"
             "gap-4"
             "border"
             "border-slate-600"
             "bg-slate-800"
             "font-mono"
             "p-4"]}

    [:div
     {:class ["inline-flex" "items-center" "col-span-4"]}
     [:div
      {:class ["text-slate-400" "pr-4"]}
      [components.debug/raw-metadata
       {:label [:span
                {:class ["text-slate-400"]}
                (components.garden/status-icon todo)]} todo]]

     [components.garden/text-with-links (:org/name todo)]]

    [queued todo]

    [:div
     {:class ["justify-self-end"]}
     [components.garden/tags-comp todo]]

    [:div
     {:class ["justify-self-end"]}
     [floating/popover
      {:hover  true :click true
       :offset 0
       :anchor-comp
       [:div
        {:class ["cursor-pointer" "text-slate-400"
                 ]}
        (when (seq (:org/parent-name todo))
          [:div
           [components.garden/text-with-links
            (str
              (-> todo :org/parent-name)
              " :: "
              (:org/short-path todo))]])]
       :popover-comp
       [:div
        {:class ["p-4"
                 "bg-slate-800"
                 "border" "border-slate-900"]}
        [components.garden/full-note todo]]}]]

    [:div
     {:class ["justify-self-end" "col-span-2"]}
     [components.actions/actions-list
      {:actions (handlers/->actions todo)
       :n       5}]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label n] :as opts} todos]
  (let [n    (or n (count todos))
        n    (if (> n (count todos)) (count todos) n)
        na   (uix/state n)
        step 3]
    (when (seq todos)
      [:div {:class ["grid" "grid-flow-row" "place-items-center"]}
       [:div {:class ["p-2" "pt-4" "grid" "grid-flow-col" "w-full"]}
        [:div {:class ["text-2xl"]}
         label]
        [:div
         {:class ["self-center" "justify-self-end"]}
         [components.actions/actions-list
          {:actions
           [(when (> @na step)
              {:action/label    "show less"
               :action/on-click (fn [_] (swap! na #(- % step)))})
            (when (< @na (count todos))
              {:action/label    "show more"
               :action/on-click (fn [_] (swap! na #(+ % step)))})
            (when (< @na (count todos))
              {:action/label    (str "show all (" (count todos) ")")
               :action/on-click (fn [_] (reset! na (count todos)))})]}]]]

       [:div {:class ["grid" "grid-flow-row"
                      "w-full"
                      "px-4"]}
        (for [[i td] (->> todos
                          (sort-by :org/parent-name >)
                          (take @na)
                          (map-indexed vector))]
          ^{:key i}
          [todo-row opts td])]])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-tags-list
  [it tags]
  (let [tags (set/difference tags (-> it note/->all-tags (into #{})))]
    (when (seq tags)
      [:div
       {:class ["p-4"
                "bg-yo-blue-800"
                "text-md" "font-mono"
                "flex" "flex-row" "flex-wrap"]}
       ":"
       (for [[i tag] (->> tags (map-indexed vector))]
         ^{:key i}
         [:span
          [:span
           {:class
            (concat ["cursor-pointer"]
                    (colors/color-wheel-classes
                      {:type :line :i (+ 2 i)})
                    (colors/color-wheel-classes
                      {:type :both :i i :hover? true}))
            :on-click (fn [_] (handlers/add-tag
                                (dissoc it :actions/inferred)
                                tag))}
           tag]
          ":"])])))

(defn todo->add-tag-actions [todo tags opts]
  (cond
    (:no-popup opts)
    (->> (set/difference tags (-> todo note/->all-tags (into #{})))
         (map
           (fn [tag]
             {:action/on-click
              (fn []
                (handlers/add-tag (dissoc todo :actions/inferred) tag))
              :action/label (str "Add Tag " tag)})))

    :else
    [{:action/type  :action/popup
      :action/label "Add Tag"
      :action/popup-comp
      ^{:key (:db/id todo)}
      [:div [add-tags-list todo tags]]}]))

(defn item->suggested-tags [{:as   _item
                             :keys [:org/name-string
                                    :org/parent-name]}]
  (let [all-db-tags
        ;; TODO pull from fe-db/backend?
        #{"clawe" "godot" "dino" "gunner" "blog" "patrons"}]
    (->> all-db-tags
         (filter
           (fn [tag]
             (or
               (string/includes? name-string tag)
               (string/includes? parent-name tag)))))))

(defn todo->inferred-actions
  ([todo] (todo->inferred-actions todo nil))
  ([todo opts]
   (let [tags
         (->>
           (concat
             (:sibling-tags opts)
             (item->suggested-tags todo))
           (into #{}))]
     ;; NOTE attaching non transit properties breaks defhandlers
     (update todo :actions/inferred concat
             (when (seq tags)
               (todo->add-tag-actions todo tags opts))))))

;; consider 'suggestions' naming (vs 'inferred')
(defn infer-actions
  ([todos] (infer-actions nil todos))
  ([opts todos]
   (let [tags (->> todos (mapcat note/->all-tags) (into #{}))]
     (->> todos (map #(todo->inferred-actions
                        % (assoc opts :sibling-tags tags)))))))
