(ns doctor.ui.views.focus
  (:require
   [uix.core.alpha :as uix]
   [dates.tick :as dates]
   [doctor.ui.hooks.use-focus :as use-focus]
   [doctor.ui.handlers :as handlers]
   [components.actions :as components.actions]
   [components.colors :as colors]
   [components.filter :as components.filter]
   [components.garden :as components.garden]
   [components.debug :as components.debug]
   [components.todo :as todo]
   [components.filter-defs :as filter-defs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo status

(defn todo-status [it]
  (todo/status
    {:on-click
     (fn [_]
       ;; TODO refactor this logic into...something?
       ;; needs to support going from one state to another... maybe via a popup menu, with a default
       (handlers/todo-set-new-status
         it (cond
              (todo/completed? it)   :status/not-started
              (todo/skipped? it)     :status/not-started
              (todo/current? it)     :status/done
              (todo/in-progress? it) :status/done
              (todo/not-started? it) :status/in-progress)))}
    it))

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
         [:span {:class
                 (concat
                   (colors/color-wheel-classes {:type :line :i i})
                   ["whitespace-nowrap"])}
          " " nm]))
     (interpose [:span
                 {:class ["text-city-blue-dark-200" "px-4"]}
                 " > "])
     ;; kind of garbage, but :shrug:
     (map-indexed (fn [i sp] ^{:key i}
                    [:span sp])))])

(defn parent-names
  ([it] (parent-names nil it))
  ([{:keys [header? n]} it]
   (let [p-names      (-> it :org/parent-names)
         p-names      (cond->> p-names
                        n (take n))
         p-name       (-> p-names first)
         rest-p-names (-> p-names rest)]
     (if-not header?
       [breadcrumbs p-names]
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "items-center"]}
        [breadcrumbs rest-p-names]
        [:span
         {:class ["text-city-blue-dark-200" "px-4"]}
         " > "]
        [:span
         {:class ["font-nes" "text-xl" "p-3"
                  "text-city-green-400"
                  "whitespace-nowrap"
                  ]}
         p-name]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item-id-hash

(defn item-id-hash [it]
  [:div
   {:class
    (concat
      ["flex" "text-sm" "font-nes"
       "text-city-red-300" "ml-2"
       "hover:opacity-100"]
      (when-not (:org/id it)
        ["opacity-50" "cursor-pointer" "tooltip"]))
    :on-click (fn [_] (when-not (:org/id it)
                        (handlers/ensure-uuid it)))}
   (if (:org/id it)
     (->> it :org/id str (take 4) (apply str))
     [:span
      [:span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
       "ensure-uuid"]
      "####"])])

(defn db-id [it]
  (when (:db/id it)
    [:div
     {:class
      ["flex" "text-sm" "font-nes" "ml-2"
       "text-city-green-300"
       "hover:text-slate-400"
       "hover:line-through"
       "cursor-pointer" "tooltip"]
      :on-click #(handlers/delete-from-db it)}
     [:span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
      "delete-from-db"]
     (->> it :db/id str (apply str))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defn item-header [it]
  [:div
   {:class ["flex" "flex-col" "px-4"
            "text-city-green-400" "text-xl"]}

   [:div {:class ["flex" "flex-row" "items-center"]}
    [todo/level it]
    [todo-status it]
    [item-id-hash it]
    [:div {:class ["ml-auto"]}
     [todo/tags-list
      {:on-click (fn [tag] (use-focus/remove-tag it tag))}
      it]]
    [todo/priority-label
     {:on-click (fn [_] (use-focus/remove-priority it))}
     it]]

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
         (todo/completed? it) ["text-city-blue-dark-400"]
         (todo/skipped? it)   ["text-city-blue-dark-600"]
         ;; (not-started? it) []
         :else                ["text-city-blue-dark-200"]))}

    ;; top meta
    [:div
     {:class ["flex" "flex-row" "w-full" "items-center"]}

     [todo/level it]
     [todo-status it]
     [db-id it]
     [item-id-hash it]
     [:div {:class ["ml-auto"]}
      [todo/priority-label {:on-click (fn [_] (use-focus/remove-priority it))} it]]]

    ;; middle content
    [:div
     {:class ["flex" "flex-col" "pb-2"]}

     [todo/tags-list
      {:on-click (fn [tag] (use-focus/remove-tag it tag))}
      it]

     ;; name
     [:span
      [:span
       {:class (when (or (todo/completed? it) (todo/skipped? it)) ["line-through"])}
       (:org/name-string it)]]

     ;; time ago
     (when (and (todo/completed? it) (:org/closed it))
       [:span
        {:class ["font-mono"]}
        (str
          (some-> it :org/closed
                  dates.tick/parse-time-string
                  dates.tick/human-time-since)
          " ago")])

     (when-not hide-parent-names?
       [parent-names {:n 2} it])]

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sort todos

(defn sort-todos [its]
  (->> its
       (sort-by
         (fn [it]
           (cond->
               0
             ;; move finished to back
             (or (todo/completed? it)
                 (todo/skipped? it)) (+ 1000)

             ;; sort by priority
             (:org/priority it)
             (+ (components.filter/label->comparable-int (:org/priority it)))

             (not (:org/priority it))
             (+ 100)

             ;; move current to front
             (or (todo/current? it)
                 (todo/in-progress? it)) (- 100)))
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
        [:div {:class (concat ["flex" "flex-col"]
                              (when children?
                                ["border-city-green-400" "border"
                                 "bg-city-blue-900"
                                 "py-3"]))}
         [:div {:class ["p-2"]}
          (when children?
            [:div
             {:class ["flex" "flex-col"]}
             [:div
              {:class ["flex" "flex-row" "items-center"]}

              [todo-status item]
              [db-id item]
              [item-id-hash item]
              [:div {:class ["ml-auto"]}
               [todo/tags-list
                {:on-click (fn [tag] (use-focus/remove-tag item tag))}
                item]]
              [todo/priority-label {:on-click (fn [_] (use-focus/remove-priority item))} item]]

             [:div
              {:class ["flex" "flex-row" "items-center" "px-3"]}
              [parent-names {:header? true} (first grouped-todos)]

              [:span
               {:class ["ml-auto"]}
               [components.actions/actions-list
                {:actions       (handlers/->actions item (handlers/todo->actions item))
                 :nowrap        true
                 :hide-disabled true}]]]])]

         (if children?
           [:div
            {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}
            (for [[i td] (->> grouped-todos sort-todos (map-indexed vector))]
              ^{:key i}
              [:div
               {:class ["p-2"]}
               [item-card {:hide-parent-names? true} td]])]
           [:div
            {:class ["p-2"]}
            [item-card {:hide-parent-names? false} item]])])])))

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
           [item-todo-cards
            {:filter-by (comp not #{:status/skipped :status/done} :org/status)}
            c]
           [item-body c]]))

      (when
          ;; this could also check commit status, dirty/unpushed commits, etc
          (and (seq todos)
               (->> todos (filter todo/current?) seq not))
        [:div
         {:class ["text-bold" "text-city-pink-300" "p-4"]}
         [:h1
          {:class ["text-4xl" "font-nes"]}
          "no :current: todo!"]
         [:p
          {:class ["text-2xl" "pt-4"]}
          "What are ya, taking a load off? GERT BERK TER WORK!"]])])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter-grouper presets

(defn presets []
  ;; these presets might be higher level modes, i.e. they might imply other ui changes
  {:repo
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       ["russmatney/clawe"
        "russmatney/dino"
        "russmatney/org-crud"]}}
    :group-by    :filters/short-path
    :sort-groups :filters/short-path}

   :clawe
   {:filters
    ;; TODO add the clawe workspace here as well
    #{{:filter-key :filters/short-path :match-str-includes-any ["russmatney/clawe"]}
      ;; TODO support OR here, probably opt-in
      ;; {:filter-key :filters/tags :match "clawe"}
      }
    :group-by    :filters/priority
    :sort-groups :filters/priority}
   :org-crud
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any ["russmatney/org-crud"]}
      ;; {:filter-key :filters/tags :match "orgcrud"}
      }
    :group-by    :filters/priority
    :sort-groups :filters/priority}
   :dino
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any ["russmatney/dino"]}
      ;; {:filter-key :filters/tags :match "dino"}
      }
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :incomplete
   {:filters
    #{{:filter-key :filters/status :match :status/not-started}
      {:filter-key :filters/status :match :status/in-progress}}
    :group-by    :filters/priority
    :sort-groups :filters/priority
    :label       "Incomplete"
    :default     true}

   :prioritized
   {:filters
    #{{:filter-key :filters/priority :match-fn (comp not nil?)}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :prioritized-incomplete
   {:filters
    #{{:filter-key :filters/status :match :status/not-started}
      {:filter-key :filters/status :match :status/in-progress}
      {:filter-key :filters/priority :match-fn (comp not nil?)}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :unprioritized
   {:filters
    #{{:filter-key :filters/priority :match-fn nil?}}
    :group-by    :filters/short-path
    :sort-groups :filters/short-path}

   :tagged-current
   {:filters     #{{:filter-key :filters/tags :match "current"}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :today
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any #{(filter-defs/short-path-days-ago 0)}}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :today-complete
   {:filters
    #{{:filter-key :filters/status :match :status/done}
      {:filter-key :filters/short-path :match-str-includes-any #{(filter-defs/short-path-days-ago 0)}}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :today-incomplete
   {:filters
    #{{:filter-key :filters/status :match :status/in-progress}
      {:filter-key :filters/status :match :status/not-started}
      {:filter-key :filters/short-path :match-str-includes-any #{(filter-defs/short-path-days-ago 0)}}}
    :group-by    :filters/priority
    :sort-groups :filters/priority}

   :last-three-days
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       (->> 3 range (map filter-defs/short-path-days-ago))}}
    :group-by    :filters/short-path
    :sort-groups :filters/short-path}

   :last-seven-days
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       (->> 7 range (map filter-defs/short-path-days-ago))}}
    :group-by    :filters/short-path
    :sort-groups :filters/short-path}

   :tags
   {:filters     {}
    :group-by    :filters/tags
    :sort-groups :filters/tags}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn widget [opts]
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data

        hide-completed (uix/state (:hide-completed opts))
        only-current   (uix/state (:only-current opts))
        pills
        [{:on-click #(swap! hide-completed not)
          :label    (if @hide-completed "Show completed" "Hide completed")
          :active   @hide-completed}
         {:on-click #(swap! only-current not)
          :label    (if @only-current "Show all" "Show only current")
          :active   @only-current}]

        filter-data (components.filter/use-filter
                      (-> filter-defs/fg-config
                          (assoc :items todos
                                 :show-filters-inline true
                                 :extra-preset-pills pills
                                 :filter-items (fn [items]
                                                 (cond->> items
                                                   @hide-completed (remove todo/completed?)
                                                   @only-current   (filter todo/current?)))
                                 :sort-items sort-todos)
                          (update :presets merge (presets))))
        current     (some->> todos (filter todo/current?) seq)]
    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"]}

     (if (:only-current-stack opts)
       [current-stack current]
       [:div
        {:class ["flex" "flex-col" "min-h-screen"]}

        [:hr {:class ["mb-6" "border-city-blue-900"]}]
        [:div
         {:class ["px-6"
                  "text-city-blue-400"]}

         (:filter-grouper filter-data)]

        (when (seq (:filtered-items filter-data))
          [:div {:class ["pt-6"]}
           [components.filter/items-by-group
            (assoc filter-data :item->comp item-todo-cards)]])

        (when (not (seq todos))
          [:div
           {:class ["text-bold" "text-city-pink-300" "p-4"]}
           [:h1
            {:class ["text-4xl" "font-nes"]}
            "no todos found!"]
           [:p
            {:class ["text-2xl" "pt-4"]}
            ""]])])]))
