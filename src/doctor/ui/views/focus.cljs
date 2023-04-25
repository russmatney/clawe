(ns doctor.ui.views.focus
  (:require
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [components.todo :as todo]
   [components.item :as item]
   [uix.core.alpha :as uix]
   [hiccup-icons.fa :as fa]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current task (for topbar)

(defn current-task [{:keys [conn]}]
  (let [current-todos (ui.db/current-todos conn)]
    (if-not (seq current-todos)
      [:span "--"]
      (let [todos   (todo/sort-todos current-todos)
            n       (uix/state 0)
            current (get (into [] todos) @n)
            ct      (count todos)]
        [:div
         {:class ["flex" "flex-row" "place-self-center"
                  "items-center" "space-x-4"
                  "h-full"]}

         [:span
          {:class ["pl-3" "font-mono"]}
          (str (inc @n) "/" ct)]

         [:div
          {:class ["font-mono pr-3" "whitespace-nowrap"]}
          [components.garden/text-with-links (:org/name current)]]

         [components.actions/actions-list
          {:n 2 :hide-disabled true
           :actions
           (concat
             (when (> ct 0)
               [{:action/label    "next"
                 :action/icon     fa/chevron-up-solid
                 :action/disabled (>= @n (dec ct))
                 :action/on-click (fn [_] (swap! n inc))
                 :action/priority 5}
                {:action/label    "prev"
                 :action/icon     fa/chevron-down-solid
                 :action/disabled (zero? @n)
                 :action/on-click (fn [_] (swap! n dec))
                 :action/priority 5}])
             (handlers/->actions current))}]]))))

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
     [todo/tags-list it]]
    [todo/priority-label it]]

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

(defn current-stack [todos]
  (when (seq todos)
    [:div
     (for [[i c] (->> todos todo/sort-todos (map-indexed vector))]
       ^{:key i}
       [:div
        {:class ["bg-city-blue-800"]}
        [:hr {:class ["border-city-blue-900" "pb-4"]}]
        [item-header c]
        [todo/card-or-card-group
         {:filter-by
          (comp not #{:status/skipped :status/done} :org/status)}
         c]
        [item-body c]])]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn widget [opts]
  (let [todos (ui.db/current-todos (:conn opts))]
    (if (seq todos)
      [current-stack todos]
      [:div
       {:class ["text-center" "my-36" "text-slate-200"]}
       [:div {:class ["font-nes"]} "No current task!"]
       [:div {:class ["font-mono"]} "Maybe take a load off?"]])))
