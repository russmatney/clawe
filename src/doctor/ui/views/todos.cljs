(ns doctor.ui.views.todos
  (:require
   [uix.core :as uix :refer [$ defui]]
   [taoensso.telemere :as log]

   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]
   [components.filter :as components.filter]
   [components.todo :as todo]
   [components.filter-defs :as filter-defs]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [components.item :as item]))

(defn todos-table-def []
  {:headers ["" "Name" "Tags" "Parents" "Actions"]
   :->row   (fn [todo]
              [($ :span.flex.flex-row.items-center.space-x-2
                  ($ todo/level {:item todo})
                  ($ todo/status {:item todo})
                  ($ todo/priority-label {:item todo})
                  ($ item/db-id {:item todo})
                  ($ item/id-hash {:item todo}))

               ($ :span
                  {:class ["flex-grow" "font-mono"]}
                  (:org/name-string todo))

               ($ components.garden/all-nested-tags-comp {:item todo})
               ($ item/parent-names {:item todo})
               ($ components.actions/actions-list
                  {:actions (handlers/->actions todo)
                   :n       7})])})

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

   ;; :tagged-current
   ;; {:filters     #{{:filter-key :filters/tags :match "current"}}
   ;;  :group-by    :filters/priority
   ;;  :sort-groups :filters/priority}

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

   :last-30-days
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       (->> 30 range (map filter-defs/short-path-days-ago))}}
    :group-by    :filters/short-path
    :sort-groups :filters/short-path}

   :tags
   {:filters     {}
    :group-by    :filters/tags
    :sort-groups :filters/tags}

   :unprioritized-by-tag
   {:filters
    #{{:filter-key :filters/priority :match-fn nil?}}
    :group-by    :filters/tags
    :sort-groups :filters/tags}

   })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defui widget [opts]
  (let [{:keys [data]}
        (hooks.use-db/use-query
          {:db->data
           #(ui.db/list-todos % {:join-children? true :skip-subtasks? true})})
        todos                                   data
        [only-incomplete set-only-incomplete]   (uix/use-state (:only-incomplete opts))
        [hide-in-progress set-hide-in-progress] (uix/use-state (:hide-in-progress opts))
        pills
        [{:on-click #(set-hide-in-progress not)
          :label    (if hide-in-progress "Show in-progress" "Hide in-progress")
          :active   hide-in-progress}
         {:on-click #(set-only-incomplete not)
          :label    (if only-incomplete "All" "Only Incomplete")
          :active   only-incomplete}]

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
                                  only-incomplete
                                  (filter #(or (todo/not-started? %)
                                               (todo/in-progress? %)))
                                  hide-in-progress
                                  (filter #(not (todo/in-progress? %)))))
                :sort-items todo/sort-todos)
              (update :presets merge (presets))))]

    (log/log! {:data {:todos (count todos)}} "todos rendering")
    ($ :div
       {:class ["bg-city-blue-800" "bg-opacity-90"
                "flex" "flex-col" "mb-8"]}

       ($ :hr {:class ["mb-6" "border-city-blue-900"]})
       ($ :div
          {:class ["px-6"
                   "text-city-blue-400"]}

          (:filter-grouper filter-data))

       (when (seq (:filtered-items filter-data))
         ($ :div {:class ["pt-6"]}
            ($ components.filter/items-by-group
               (assoc filter-data
                      :xs->xs todo/infer-actions
                      :table-def (todos-table-def)
                      :item->comp (fn [opts] ($ todo/card-or-card-group opts))))))

       (when (not (seq todos))
         ($ :div
            {:class ["text-bold" "text-city-pink-300" "p-4"]}
            ($ :h1
               {:class ["text-4xl" "font-nes"]}
               "no todos found!")
            ($ :p
               {:class ["text-2xl" "pt-4"]}
               ""))))))
