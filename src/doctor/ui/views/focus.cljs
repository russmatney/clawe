(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [doctor.ui.handlers :as handlers]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [components.todo :as todo]
   [components.item :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defn item-header [it]
  [:div
   {:class ["flex" "flex-col" "px-4"
            "text-city-green-400" "text-xl"]}

   [:div {:class ["pb-2" "flex" "flex-row"
                  "items-center" "justify-between"]}
    [item/parent-names it]
    [components.actions/actions-list
     {:actions       (handlers/->actions it (handlers/todo->actions it))
      :nowrap        true
      :hide-disabled true}]]

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
    [components.garden/org-body it]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current stack

(defn current-stack
  ([current] [current-stack nil current])
  ([{:keys [todos]} current]
   (let [todos (or todos [])]
     [:div
      (when current
        (for [[i c] (->> current todo/sort-todos (map-indexed vector))]
          ^{:key i}
          [:div
           {:class ["bg-city-blue-800"]}
           [:hr {:class ["border-city-blue-900" "pb-4"]}]
           [item-header c]
           [todo/card-or-card-group
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
;; main widget

(defn widget [_opts]
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data
        current-todos   (some->> todos (filter todo/current?) seq)]
    [current-stack current-todos]))
