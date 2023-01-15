(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [clojure.set :as set]
   [uix.core.alpha :as uix]
   [tick.core :as t]
   [dates.tick :as dates]
   [doctor.ui.pomodoros :as pomodoros]
   [components.actions :as components.actions]
   [doctor.ui.handlers :as handlers]
   [components.colors :as colors]
   [components.filter :as components.filter]
   [components.garden :as components.garden]
   [pages.todos :as pages.todos]
   [components.debug :as components.debug]))

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

(defn current? [it]
  (seq (set/intersection #{"current"} (:org/tags it))))

;; (defn has-tag? [it tag]
;;   (seq (set/intersection #{tag} (:org/tags it))))

;; (defn has-tags? [it tags]
;;   (seq (set/intersection tags (:org/tags it))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; level

(defn level [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    [:div
     {:class ["whitespace-nowrap" "font-nes"]}
     (->> (repeat level "*") (apply str))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo status

(defn todo-status [it]
  (when (:org/status it)
    (let [hovering? (uix/state nil)]
      [:span
       {:class          ["ml-2" "whitespace-nowrap" "font-nes"
                         "cursor-pointer"
                         "hover:opacity-50"]
        :on-mouse-enter (fn [_] (reset! hovering? true))
        :on-mouse-leave (fn [_] (reset! hovering? false))
        :on-click
        (fn [_]
          ;; TODO refactor this logic into...something?
          (handlers/todo-set-new-status
            it (cond
                 (completed? it)   :status/not-started
                 (skipped? it)     :status/not-started
                 (current? it)     :status/done
                 (in-progress? it) :status/done
                 (not-started? it) :status/in-progress)))}
       (cond
         (and (completed? it) (not @hovering?))   "[X]"
         (and (completed? it) @hovering?)         "[ ]"
         (and (skipped? it) (not @hovering?))     "SKIP"
         (and (skipped? it) @hovering?)           "[ ]"
         (and (current? it) (not @hovering?))     "[-]"
         (and (current? it) @hovering?)           "[X]"
         (and (in-progress? it) (not @hovering?)) "[-]"
         (and (in-progress? it) @hovering?)       "[X]"
         (and (not-started? it) (not @hovering?)) "[ ]"
         (and (not-started? it) @hovering?)       "[-]")])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags-list

(defn tags-list [it]
  (when (seq (:org/tags it))
    [:span
     {:class ["text-md" "font-mono"
              "px-2"
              "flex" "flex-row" "flex-wrap"]}
     ":"
     (for [[i t] (->> it :org/tags (map-indexed vector))]
       ^{:key t}
       [:span
        [:span
         {:class
          (concat ["cursor-pointer" "hover:line-through"]
                  (colors/color-wheel-classes {:type :line :i (+ 2 i)}))
          :on-click (fn [_ev] (use-focus/remove-tag it t))}
         t]
        ":"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; priority-label

(defn priority-label [it]
  (when (:org/priority it)
    [:span
     {:class
      (concat
        ["whitespace-nowrap" "font-nes"]
        (let [pri (:org/priority it)]
          (cond
            (completed? it) ["text-city-blue-dark-400"]
            (skipped? it)   ["text-city-blue-dark-400"]
            (#{"A"} pri)    ["text-city-red-400"]
            (#{"B"} pri)    ["text-city-pink-400"]
            (#{"C"} pri)    ["text-city-green-400"])))}
     (str "#" (:org/priority it) " ")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parent-names

(defn breadcrumbs [bcrumbs]
  [:span
   {:class ["flex" "flex-row"]}
   (->>
     bcrumbs
     reverse
     (map-indexed
       (fn [i nm]
         [:span {:class (colors/color-wheel-classes {:type :line :i i})}
          " " nm]))
     (interpose [:span
                 {:class ["text-city-blue-dark-200" "px-4"]}
                 " > "])
     ;; kind of garbage, but :shrug:
     (map-indexed (fn [i sp] ^{:key i}
                    [:span sp])))])

(defn parent-names
  ([it] (parent-names nil it))
  ([{:keys [header?]} it]
   (let [p-names      (-> it :org/parent-names)
         p-name       (-> p-names first)
         rest-p-names (-> p-names rest)]
     (if-not header?
       [breadcrumbs p-names]
       [:div
        {:class ["flex" "flex-col"]}
        [breadcrumbs rest-p-names]
        [:span
         {:class ["font-nes" "text-3xl" "p-3"
                  "text-city-green-400"]}
         p-name]
        [:div
         {:class ["flex" "flex-"]}]
        ])
     )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item-id-hash

(defn item-id-hash [it]
  (when (:org/id it)
    [:div
     {:class ["flex" "text-sm" "font-nes"
              "text-city-red-300" "ml-2"]}
     (->> it :org/id str (take 4) (apply str))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defn item-header [it]
  [:div
   {:class ["flex" "flex-col" "px-4"
            "text-city-green-400" "text-xl"]}

   [:div {:class ["flex" "flex-row" "items-center"]}
    [level it]
    [todo-status it]
    [item-id-hash it]
    [:div {:class ["ml-auto"]}
     [tags-list it]]
    [priority-label it]]

   [:div {:class ["flex" "flex-row"]}
    [:div {:class ["font-nes"]}
     (:org/name-string it)]]])

(defn item-body [it]
  [:div
   {:class ["text-xl" "p-4" "flex" "flex-col"]}
   [:div
    {:class ["text-yo-blue-200" "font-mono"]}
    ;; TODO include sub todos
    #_[:pre (:org/body-string it)]
    [components.garden/org-body it]]

   [:div {:class ["py-4" "flex" "flex-row" "justify-between"]}
    [parent-names it]
    [components.actions/actions-list
     {:actions
      (handlers/->actions it (handlers/todo->actions it))
      :nowrap        true
      :hide-disabled true}]]])

(defn item-card
  ([it] (item-card nil it))
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
     [todo-status it]
     [item-id-hash it]
     [:div {:class ["ml-auto"]}
      [tags-list it]]
     [priority-label it]]

    ;; middle content
    [:div
     {:class ["flex" "flex-col" "pb-2"]}

     ;; name
     [:span
      [:span
       {:class (when (or (completed? it) (skipped? it)) ["line-through"])}
       (:org/name-string it)]]

     ;; time ago
     (when (and (completed? it) (:org/closed-since it))
       [:span
        {:class ["font-mono"]}
        (str (:org/closed-since it) " ago")])

     (when-not hide-parent-names?
       [parent-names it])]

    ;; bottom meta
    [:div
     {:class ["flex" "flex-row"
              "items-center"
              "text-sm"
              "mt-auto"
              "pb-2"]}

     [components.debug/raw-metadata {:label "RAW"} it]

     ;; actions list
     [:span
      {:class ["ml-auto"]}
      [components.actions/actions-list
       {:actions
        (handlers/->actions it (handlers/todo->actions it))
        :nowrap        true
        :hide-disabled true}]]]]))

(defn button [opts label]
  [:button
   (merge
     {:class ["cursor-pointer"
              "bg-city-blue-900"
              "text-city-green-200"
              "text-xl"
              "py-2" "my-2"
              "px-4" "mx-4"
              "rounded"]}
     opts)
   label])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bar

(defn bar [{:keys [time]}]
  [:div
   {:class ["flex flex-row" "items-center"
            "bg-city-blue-700"
            "text-city-green-200"]}
   [:div
    {:class ["text-2xl" "font-nes" "pl-4"]}
    (str
      (t/format
        (t/formatter "h:mma")
        (dates/add-tz time)))]

   (let [p-state                (pomodoros/get-state)
         {:keys [current last]} p-state]
     [:div
      {:class ["ml-auto" "flex" "flex-row"]}

      (when last
        (let [{:keys [started-at finished-at]} last]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Last: " (dates/human-time-since started-at finished-at)]]))

      (when last
        (let [{:keys [finished-at]} last
              latest                (:started-at current)]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Break: " (dates/human-time-since finished-at latest)]]))

      (when current
        (let [{:keys [started-at]} current
              minutes              (t/minutes (dates/duration-since started-at))
              too-long?            (> minutes 40)]
          [:div
           {:class ["py-2" "px-4"]}
           "Current: "
           [:span
            {:class (when too-long? ["text-city-pink-400" "font-nes" "font-bold"
                                     "pl-2" "text-lg" "whitespace-nowrap"])}
            (dates/human-time-since started-at)
            (when too-long? "!!")]]))

      ;; buttons
      (->> (pomodoros/actions)
           (map (fn [{:keys [on-click label]}]
                  [button {:on-click on-click} label]))
           (into [:div]))])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; page toggles

(defn toggles
  ;; TODO rewrite as actions-based api (duh)
  ;; conditionally hide only-current when there is none
  [{:keys [hide-completed toggle-hide-completed
           only-current toggle-only-current]}]
  [:div
   [button {:on-click (fn [_] (toggle-hide-completed))}
    (if hide-completed "Show completed" "Hide completed")]
   [button {:on-click (fn [_] (toggle-only-current))}
    (if only-current "Show all" "Show only current")]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sort todos

(defn ->comparable-int [p]
  (cond
    (and p (string? p)) (.charCodeAt p)
    (int? p)            p
    (keyword? p)        (->comparable-int (name p))
    ;; some high val
    :else               1000))

(defn sort-todos [its]
  (->> its
       (sort-by
         (fn [it]
           (cond->
               0
             ;; move finished to back
             (or (completed? it)
                 (skipped? it)) (+ 1000)

             ;; sort by priority
             (:org/priority it)
             (+ (->comparable-int (:org/priority it)))

             (not (:org/priority it))
             (+ 100)

             ;; move current to front
             (or (current? it)
                 (in-progress? it)) (- 100)))
         ;; lower number means earlier in the order
         <)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item todo cards

(defn item-todo-cards
  ([item] [item-todo-cards nil item])
  ([{:keys [filter-by]} item]
   (let [todos             (->> item
                                (tree-seq (comp seq :org/items) :org/items)
                                (remove nil?)
                                (remove #(#{item} %))
                                (filter :org/status)
                                seq)
         [children? todos] (if (seq todos) [true todos] [false [item]])
         todos             (if filter-by (filter filter-by todos) todos)
         groups            (group-by (fn [it] (-> it :org/parent-names str)) todos)]
     [:div {:class ["flex" "flex-col"]}

      (for [[pnames grouped-todos] groups]
        ^{:key (str pnames)}
        [:div {:class ["flex" "flex-col"]}
         [:div {:class ["px-3" "pt-2"]}
          (when children?
            [parent-names {:header? true} (first grouped-todos)])]

         (if children?
           [:div
            {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}
            (for [[i td] (cond->> grouped-todos
                           true sort-todos
                           true (map-indexed vector))]
              ^{:key i}
              [:div
               {:class ["p-2"]}
               [item-card {:hide-parent-names? true} td]])]
           [:div
            {:class ["p-2"]}
            [item-card {:hide-parent-names? true} item]]
           )])])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current stack

(defn current-stack
  ([current] [current-stack nil current])
  ([{:keys [todos]} current]
   (let [todos (or todos [])]
     [:div
      (when current
        (for [[i c] (->> current sort-todos (map-indexed vector))]
          ^{:key i}
          [:div
           {:class ["bg-city-blue-800"]}
           [:hr {:class ["border-city-blue-900" "pb-4"]}]
           [item-header c]
           [item-todo-cards {:filter-by (comp not #{:status/skipped
                                                    :status/done}
                                              :org/status)}
            c]
           [item-body c]]))

      (when
          ;; this could also check commit status, dirty/unpushed commits, etc
          (and (seq todos)
               (->> todos (filter current?) seq not))
        [:div
         {:class ["text-bold" "text-city-pink-300" "p-4"]}
         [:h1
          {:class ["text-4xl" "font-nes"]}
          "no :current: todo!"]
         [:p
          {:class ["text-2xl" "pt-4"]}
          "What are ya, taking a load off? GERT BERK TER WORK!"]])])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter-grouper config

(defn filter-grouper-config [{:keys [todos]}]
  {:items            todos
   :all-filter-defs  pages.todos/all-filter-defs
   :default-filters  #{{:filter-key :status :match :status/not-started}
                       {:filter-key :status :match :status/in-progress}}
   :default-group-by :priority

   ;; TODO support this, and include :presets as another filter
   :preset-filter-groups
   {:default
    {:filters
     #{{:filter-key :status :match :status/not-started}
       {:filter-key :status :match :status/in-progress}}
     :group-by :priority}

    :today
    {:filters
     #{{:filter-key :short-path :match :daily/today}}
     :group-by :priority}

    :last-three-days
    {:filters
     #{{:filter-key :short-path :match :daily/today}
       {:filter-key :short-path :match :daily/yesterday}
       {:filter-key :short-path :match :daily/days-ago-3}}
     :group-by :short-path}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn widget [opts]
  ;; TODO the 'current' usage in this widget could be a 'tag' based feature
  ;; i.e. based on arbitrary tags, e.g. if that's our 'mode' right now
  ;; i.e. 'current' is an execution mode - another mode might be pre or post execution
  (let [focus-data           (use-focus/use-focus-data)
        {:keys [todos]}      @focus-data
        filter-todos-results (components.filter/use-filter
                               (filter-grouper-config {:todos todos}))
        current              (some->> todos (filter current?) seq)

        time           (uix/state (t/zoned-date-time))
        interval       (atom nil)
        hide-completed (uix/state nil)
        only-current   (uix/state nil #_(if current true nil))]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 1000))
      (fn [] (js/clearInterval @interval)))

    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"]}

     [bar (assoc opts :time @time)]

     [:div
      {:class ["flex" "flex-row" "items-center" "py-2"]}

      ;; TODO rewrite as actions-based api
      [toggles {:toggle-hide-completed (fn [] (swap! hide-completed not))
                :hide-completed        @hide-completed
                :toggle-only-current   (fn [] (swap! only-current not))
                :only-current          @only-current}]]

     [current-stack current]

     [:hr {:class ["mb-6" "border-city-blue-900"]}]
     [:div
      {:class ["px-6"
               "text-city-blue-400"]}
      (:filter-grouper filter-todos-results)]

     (when (seq (:filtered-items filter-todos-results))
       [:div {:class ["pt-6"]}
        (for [[i {:keys [item-group label]}]
              (->> (:filtered-item-groups filter-todos-results)
                   (sort-by (comp ->comparable-int :label) <)
                   (map-indexed vector))]
          ^{:key i}
          [:div
           {:class ["flex" "flex-col"]}
           [:div
            [:hr {:class ["mt-6" "border-city-blue-900"]}]
            [:div
             {:class ["p-6"]}
             (cond
               (#{:priority} (:items-group-by filter-todos-results))
               (if label
                 [priority-label {:org/priority label}]
                 [:span
                  {:class ["font-nes" "text-city-blue-400"]}
                  "No Priority"])

               (#{:tags} (:items-group-by filter-todos-results))
               (if label
                 [tags-list {:org/tags #{label}}]
                 [:span
                  {:class ["font-nes" "text-city-blue-400"]}
                  "No tags"])

               (#{:short-path} (:items-group-by filter-todos-results))
               [:span
                {:class ["font-nes" "text-city-blue-400"]}
                [pages.todos/path->basename label]]

               :else
               [:span
                {:class ["font-nes" "text-city-blue-400"]}
                (or
                  ;; TODO parse this label to plain string with org-crud
                  (str label) "None")])]]

           [:div
            {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}

            (let [items (cond->> item-group
                          @hide-completed (remove completed?)
                          @only-current   (filter current?)
                          true            sort-todos
                          true            (map-indexed vector))]
              (for [[i it] items]
                ^{:key i}
                [item-todo-cards it]))]])])

     (when (not (seq todos))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no todos found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
