(ns doctor.ui.views.focus
  (:require
   [uix.core.alpha :as uix]
   [doctor.ui.hooks.use-focus :as use-focus]
   [doctor.ui.handlers :as handlers]
   [components.actions :as components.actions]
   [components.filter :as components.filter]
   [components.garden :as components.garden]
   [components.todo :as todo]
   [components.item :as item]
   [components.filter-defs :as filter-defs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defn item-header [it]
  [:div
   {:class ["flex" "flex-col" "px-4"
            "text-city-green-400" "text-xl"]}

   [:div {:class ["flex" "flex-row" "items-center"]}
    [todo/level it]
    [todo/status it]
    [item/db-id it]
    [item/id-hash it]
    [:div {:class ["ml-auto"]}
     [todo/tags-list
      {:on-click (fn [tag] (handlers/remove-tag it tag))}
      it]]
    [todo/priority-label
     {:on-click (fn [_] (handlers/remove-priority it))}
     it]]

   [:div {:class ["flex" "flex-row"]}
    [:span {:class ["font-nes"]}
     (:org/name-string it)]]])

(defn item-body [it]
  [:div
   {:class ["text-xl" "p-4" "flex" "flex-col"]}
   [:div
    {:class ["text-yo-blue-200" "font-mono"]}
    ;; TODO include sub items + bodies
    #_[:pre (:org/body-string it)]
    [components.garden/org-body it]]

   [:div {:class ["py-4" "flex" "flex-row" "justify-between"]}
    [item/parent-names it]
    [components.actions/actions-list
     {:actions
      (handlers/->actions it (handlers/todo->actions it))
      :nowrap        true
      :hide-disabled true}]]])

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

(defn todo-cards
  "Renders the passed todo as a card.
  If it has children (i.e. sub-tasks) they will be rendered as a group of cards."
  ([item] [todo-cards nil item])
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

              [todo/status item]
              [item/db-id item]
              [item/id-hash item]
              [:div {:class ["ml-auto"]}
               [todo/tags-list
                {:on-click (fn [tag] (handlers/remove-tag item tag))}
                item]]
              [todo/priority-label {:on-click (fn [_] (handlers/remove-priority item))} item]]

             [:div
              {:class ["flex" "flex-row" "items-center" "px-3"]}
              [item/parent-names {:header? true} (first grouped-todos)]

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
               [todo/card {:hide-parent-names? true} td]])]
           [:div
            {:class ["p-2"]}
            [todo/card {:hide-parent-names? false} item]])])])))

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
           [todo-cards
            {:filter-by
             (comp not #{:status/skipped :status/done} :org/status)}
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
            (assoc filter-data :item->comp todo-cards)]])

        (when (not (seq todos))
          [:div
           {:class ["text-bold" "text-city-pink-300" "p-4"]}
           [:h1
            {:class ["text-4xl" "font-nes"]}
            "no todos found!"]
           [:p
            {:class ["text-2xl" "pt-4"]}
            ""]])])]))
