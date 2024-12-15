(ns components.todo
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [tick.core :as t]
   [taoensso.telemere :as log]
   [uix.core :as uix :refer [$ defui]]

   [components.colors :as colors]
   [components.note :as note]
   [components.floating :as floating]
   [components.garden :as components.garden]
   [components.debug :as components.debug]
   [components.actions :as components.actions]
   [components.item :as item]
   [doctor.ui.hooks.use-selection :as hooks.use-selection]
   [doctor.ui.handlers :as handlers]
   [dates.tick :as dates.tick]
   [util :as util]))

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

(defn queued [{:keys [item]}]
  (let [todo item]
    (let [queued-at (:todo/queued-at todo)]
      (when queued-at
        ($ :div
           {:class ["text-slate-400" "font-mono"]}
           (str "queued: "
                (t/format "E h:mma" (dates.tick/add-tz (t/instant queued-at)))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; priority-label

(defui priority-label
  [{:keys [item fallback] :as opts}]
  (let [priort (:org/priority item fallback)]
    (when priort
      ($ :span
         (merge {:class
                 (concat
                   ["whitespace-nowrap" "font-nes"
                    "cursor-pointer"
                    "hover:line-through"]
                   (cond
                     (and (map? item)
                          (completed? item)) ["text-city-blue-dark-400"]
                     (and (map? item)
                          (skipped? item))   ["text-city-blue-dark-400"]
                     (#{"A"} priort)         ["text-city-red-400"]
                     (#{"B"} priort)         ["text-city-pink-400"]
                     (#{"C"} priort)         ["text-city-green-400"]))
                 :on-click #(handlers/remove-priority
                              (dissoc item :actions/inferred))}
                opts)
         (str "#" priort " ")))))

(defn priority-setter-actions
  ([{:keys [item] :as opts}]
   (concat
     (->> ["A" "B" "C"]
          (map (fn [p]
                 {:action/label    (str "#" p)
                  :action/on-click (fn [_]
                                     (-> item
                                         (dissoc :actions/inferred)
                                         (handlers/set-priority p)))})))
     [{:action/label "Clear"
       :action/on-click
       (fn [_] (handlers/remove-priority (dissoc item :actions/inferred)))}])))

(defn priority-setters
  [opts]
  ($ components.actions/actions-list
     (priority-setter-actions opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; level

(defui level [{:keys [item]}]
  (let [level (:org/level item 0)
        level (if (#{:level/root} level) 0 level)]
    ($ components.debug/raw-data
       {:label
        ($ :div
           {:class ["whitespace-nowrap" "font-nes"]}
           (->> (repeat level "*") (apply str)))
        :data item})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; status

(defui status-plain [{:keys [item]}]
  (when (:org/status item)
    ($ :span
       {:class ["ml-2" "whitespace-nowrap" "font-nes"
                "cursor-pointer"
                "text-slate-300"
                "hover:opacity-50"]}
       (cond
         (completed? item)   "[X]"
         (skipped? item)     "SKIP"
         (in-progress? item) "[-]"
         (not-started? item) "[ ]"))))

(defui status [{:keys [item]}]
  (let [[hovering? set-hovering] (uix/use-state nil)]
    (when (:org/status item)
      ($ :span
         {:class          ["ml-2" "whitespace-nowrap" "font-nes"
                           "cursor-pointer"
                           "text-slate-300"
                           "hover:opacity-50"]
          :on-mouse-enter (fn [_] (set-hovering true))
          :on-mouse-leave (fn [_] (set-hovering false))
          :on-click
          (fn [_]
            ;; TODO refactor this logic into...something?
            ;; needs to support going from one state to another... maybe via a popup menu, with a default
            (handlers/todo-set-new-status
              (dissoc item :actions/inferred)
              (cond
                (completed? item)   :status/not-started
                (skipped? item)     :status/not-started
                (in-progress? item) :status/done
                (not-started? item) :status/in-progress)))}
         (cond
           (and (completed? item) (not hovering?))   "[X]"
           (and (completed? item) hovering?)         "[ ]"
           (and (skipped? item) (not hovering?))     "SKIP"
           (and (skipped? item) hovering?)           "[ ]"
           (and (in-progress? item) (not hovering?)) "[-]"
           (and (in-progress? item) hovering?)       "[X]"
           (and (not-started? item) (not hovering?)) "[ ]"
           (and (not-started? item) hovering?)       "[-]")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags-list

(defui tags-list
  [{:keys [item]}]
  (when (seq (:org/tags item))
    ($ :span
       {:class ["text-md" "font-mono"
                "flex" "flex-row" "flex-wrap"]}
       ":"
       (for [[i tag] (->> item :org/tags (map-indexed vector))]
         ($ :span {:key i}
            ($ :span
               {:class
                (concat ["cursor-pointer" "hover:line-through"]
                        (colors/color-wheel-classes {:type :line :i (+ 2 i)}))
                :on-click (fn [tag] (handlers/remove-tag
                                      (dissoc item :actions/inferred)
                                      tag))}
               tag)
            ":")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; content popover

(defui content [{:keys [item]}]
  ($ floating/popover
     {:hover true :click true

      :anchor-comp ($ :span "content")
      :popover-comp
      ($ :div
         {:class ["grid" "grid-flow-col"]}
         ;; TODO show just this item's content
         #_ [components.garden/org-body {:item item}]
         ($ components.garden/full-note {:item item}))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; card

(defui card
  [{:keys [hide-parent-names? item]}]
  ($ :div
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
          (completed? item) ["text-city-blue-dark-400"]
          (skipped? item)   ["text-city-blue-dark-600"]
          ;; (not-started? item) []
          :else             ["text-city-blue-dark-200"]))}

     ;; top meta
     ($ :div
        {:class ["flex" "flex-row" "w-full" "items-center"]}

        ($ level {:item item})
        ($ status {:item item})
        ($ item/db-id {:item item})
        ($ item/id-hash {:item item})
        ($ :div {:class ["ml-auto"]}
           ($ priority-label {:item item})))

     ;; middle content
     ($ :div
        {:class ["flex" "flex-col" "pb-2"]}

        ($ tags-list {:item item})

        ($ queued {:item item})

        ;; name
        ($ :span
           ($ :span
              {:class (when (or (completed? item) (skipped? item)) ["line-through"])}
              (:org/name-string item)))

        ;; time ago
        (when (and (completed? item) (:org/closed item))
          ($ :span
             {:class ["font-mono"]}
             (str
               (some-> item :org/closed
                       dates.tick/parse-time-string
                       dates.tick/human-time-since)
               " ago")))

        (when-not hide-parent-names?
          ($ item/parent-names {:n 2 :item item})))

     ;; bottom meta
     ($ :div
        {:class ["flex" "flex-row"
                 "items-center"
                 "text-sm"
                 "mt-auto"
                 "pb-2"]}

        ($ content {:item item})

        ;; actions list
        ($ :span
           {:class ["ml-auto" "pl-4"]}
           ($ components.actions/actions-list
              {:actions       (handlers/->actions item)
               :hide-disabled true})))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo cards

(defui card-or-card-group
  "Renders the passed todo as a card.
  If it has children (i.e. sub-tasks) they will be rendered as a group of cards."
  [{:keys [filter-by filter-fn item i]}]
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
    ;; (log/log! {:data {:i i}} "rendering card-or-card-group")
    ($ :div {:class ["flex" "flex-col"] :key i}
       (for [[pnames grouped-todos] groups]
         ($ :div
            {:key   (str pnames)
             :class (concat ["flex" "flex-col"]
                            (when children?
                              ["border-city-green-400" "border"
                               "bg-city-blue-900"
                               "py-3"]))}

            (when children?
              ($ :div
                 {:class ["flex" "flex-col" "p-2"]}
                 ($ :div
                    {:class ["flex" "flex-row" "items-center"]}

                    ($ status {:item item})
                    ($ item/db-id {:item item})
                    ($ item/id-hash {:item item})
                    ($ :div {:class ["ml-auto"]}
                       ($ tags-list {:item item}))
                    ($ priority-label {:item item}))

                 ($ :div
                    {:class ["flex" "flex-row" "items-center" "px-3"]}
                    ($ item/parent-names {:header? true :item (first grouped-todos)})

                    ($ :span
                       {:class ["ml-auto"]}
                       ($ components.actions/actions-list
                          {:actions       (handlers/->actions item)
                           :hide-disabled true})))))

            (if children?
              ($ :div
                 {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}
                 (for [[i td] (->> grouped-todos sort-todos (map-indexed vector))]
                   ($ :div {:key i :class ["p-2"]}
                      ($ card {:hide-parent-names? true :item td}))))
              ($ :div
                 {:class ["p-2"]}
                 ($ card {:hide-parent-names? false :item item}))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-row
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui todo-row
  [{:keys [item i]}]
  (let [todo item]
    ($ :div
       {:class ["grid" "grid-flow-col" "grid-cols-8"
                "gap-4"
                "border"
                "border-slate-600"
                "bg-slate-800"
                "font-mono"
                "p-4"]
        :key   i}

       ($ :div
          {:class ["inline-flex" "items-center" "col-span-4"]}
          ($ :div
             {:class ["text-slate-400" "pr-4"]}
             ($ components.debug/raw-data
                {:label [:span
                         {:class ["text-slate-400"]}
                         ($ components.garden/status-icon {:item todo})]
                 :data  todo}))

          ($ components.garden/text-with-links {:text (:org/name todo)}))

       ($ queued {:item todo})

       ($ :div
          {:class ["justify-self-end"]}
          ($ components.garden/tags-comp {:item todo}))

       ($ :div
          {:class ["justify-self-end"]}
          ($ floating/popover
             {:hover  true :click true
              :offset 0
              :anchor-comp
              ($ :div
                 {:class ["cursor-pointer" "text-slate-400"
                          ]}
                 (when (seq (:org/parent-name todo))
                   ($ :div
                      ($ components.garden/text-with-links
                         (str
                           (-> todo :org/parent-name)
                           " :: "
                           (:org/short-path todo))))))
              :popover-comp
              ($ :div
                 {:class ["p-4"
                          "bg-slate-800"
                          "border" "border-slate-900"]}
                 ($ components.garden/full-note {:item todo}))}))

       ($ :div
          {:class ["justify-self-end" "col-span-2"]}
          ($ components.actions/actions-list
             {:actions (handlers/->actions todo)
              :n       5})))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui todo-list
  [{:keys [label n todos] :as opts}]
  (let [n           (or n (count todos))
        n           (if (> n (count todos)) (count todos) n)
        [na set-na] (uix/use-state n)
        step        3]
    (when (seq todos)
      ($ :div {:class ["grid" "grid-flow-row" "place-items-center"]}
         ($ :div {:class ["p-2" "pt-4" "grid" "grid-flow-col" "w-full"]}
            ($ :div {:class ["text-2xl"]}
               label)
            ($ :div
               {:class ["self-center" "justify-self-end"]}
               ($ components.actions/actions-list
                  {:actions
                   [(when (> na step)
                      {:action/label    "show less"
                       :action/on-click (fn [_] (set-na #(- % step)))})
                    (when (< na (count todos))
                      {:action/label    "show more"
                       :action/on-click (fn [_] (set-na #(+ % step)))})
                    (when (< na (count todos))
                      {:action/label    (str "show all (" (count todos) ")")
                       :action/on-click (fn [_] (set-na (count todos)))})]})))

         ($ :div {:class ["grid" "grid-flow-row"
                          "w-full"
                          "px-4"]}
            (for [[i td] (->> todos
                              (sort-by :org/parent-name >)
                              (take na)
                              (map-indexed vector))]
              ($ todo-row (assoc opts :key i :item td))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui add-tags-list
  [{:keys [item tags]}]
  (let [selections (hooks.use-selection/last-n-selections)
        sels       (->> selections (map #(-> % string/trim (string/replace " " ""))) (remove #{"" nil}))
        tags       (-> tags
                       (set/difference
                         (-> item note/->all-tags (into #{})))
                       (set/union (into #{} sels)))]
    (when (seq tags)
      ($ :div
         {:class ["p-4"
                  "bg-yo-blue-800"
                  "text-md" "font-mono"
                  "flex" "flex-row" "flex-wrap"]}
         ":"
         (for [[i tag] (->> tags (map-indexed vector))]
           ($ :span
              {:key i}
              ($ :span
                 {:class
                  (concat ["cursor-pointer"]
                          (colors/color-wheel-classes
                            {:type :line :i (+ 2 i)})
                          (colors/color-wheel-classes
                            {:type :both :i i :hover? true}))
                  :on-click (fn [_] (handlers/add-tag
                                      (dissoc item :actions/inferred)
                                      tag))}
                 tag)
              ":"))))))

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
      ($ :div
         {:key (:db/id todo)}
         [add-tags-list {:item todo :tags tags}])}]))

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
               (and name-string (string/includes? name-string tag))
               (and parent-name (string/includes? parent-name tag))))))))

(defn todo->inferred-actions
  ([todo] (todo->inferred-actions todo nil))
  ([todo opts]
   (let [tags
         (->>
           (concat
             (:sibling-tags opts)
             (item->suggested-tags todo)
             #{"bug" "feat" "chore" "spike"})
           (into #{}))]
     ;; NOTE attaching non transit properties breaks defhandlers
     (update todo :actions/inferred concat
             (todo->add-tag-actions todo tags opts)))))

;; consider 'suggestions' naming (vs 'inferred')
(defn infer-actions
  ([todos] (infer-actions nil todos))
  ([opts todos]
   (let [tags (->> todos (mapcat note/->all-tags) (into #{}))]
     (->> todos (map #(todo->inferred-actions
                        % (assoc opts :sibling-tags tags)))))))
