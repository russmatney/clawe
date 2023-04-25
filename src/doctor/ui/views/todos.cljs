(ns doctor.ui.views.todos
  (:require
   [uix.core.alpha :as uix]
   [doctor.ui.db :as ui.db]
   [components.filter :as components.filter]
   [components.todo :as todo]
   [components.filter-defs :as filter-defs]

   [components.floating :as floating]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [doctor.ui.handlers :as handlers]
   [components.item :as item]))

(defn todos-table-def []
  {:headers ["" "Name" "Tags" "Parents" "Actions"]
   :->row   (fn [todo]
              [[:span.flex.flex-row.items-center.space-x-2
                [todo/level todo]
                [todo/status todo]
                [todo/priority-label todo]
                [item/db-id todo]
                [item/id-hash todo]]
               [floating/popover
                {:hover        true :click true
                 :anchor-comp  [:span
                                {:class ["flex-grow" "font-mono"]}
                                (:org/name-string todo)]
                 :popover-comp [components.garden/full-note todo]}]
               [components.garden/all-nested-tags-comp todo]
               [item/parent-names todo]
               [components.actions/actions-list
                {:actions (handlers/->actions todo) :n 4}]])})

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
  (let [todos           (ui.db/list-todos
                          (:conn opts)
                          {:join-children? true
                           :skip-subtasks? true})
        only-incomplete (uix/state (:only-incomplete opts))
        hide-current    (uix/state (:hide-current opts))
        pills
        [{:on-click #(swap! hide-current not)
          :label    (if @hide-current "Show current" "Hide current")
          :active   @hide-current}
         {:on-click #(swap! only-incomplete not)
          :label    (if @only-incomplete "All" "Only Incomplete")
          :active   @only-incomplete}]

        filter-data
        (components.filter/use-filter
          (-> filter-defs/fg-config
              (assoc
                :id (:filter-id opts :views-todos-filter)
                :label (str (count todos) " Todos")
                :items todos
                :show-filters-inline true
                :extra-preset-pills pills
                :filter-items (fn [items]
                                (cond->> items
                                  @only-incomplete
                                  (filter #(or (todo/not-started? %)
                                               (todo/in-progress? %)))
                                  @hide-current
                                  (filter #(not (todo/current? %)))))
                :sort-items todo/sort-todos)
              (update :presets merge (presets))))]
    [:div
     {:class ["bg-city-blue-800" "bg-opacity-90"
              "flex" "flex-col" "mb-8"]}

     [:hr {:class ["mb-6" "border-city-blue-900"]}]
     [:div
      {:class ["px-6"
               "text-city-blue-400"]}

      (:filter-grouper filter-data)]

     (when (seq (:filtered-items filter-data))
       [:div {:class ["pt-6"]}
        [components.filter/items-by-group
         (assoc filter-data
                :table-def (todos-table-def)
                :item->comp todo/card-or-card-group)]])

     (when (not (seq todos))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no todos found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
