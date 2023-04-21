(ns doctor.ui.views.focus
  (:require
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [components.todo :as todo]
   [components.item :as item]
   [wing.core :as w]
   [uix.core.alpha :as uix]
   [dates.tick :as dt]
   [hiccup-icons.fa :as fa]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current task (for topbar)

;; TODO move to views/focus
(defn current-task [{:keys [conn]}]
  (let [current-todos (ui.db/current-todos conn)]
    (if-not (seq current-todos)
      [:span "--"]
      (let [queued  (->> current-todos
                         (w/dedupe-by :db/id)
                         (sort-by :todo/queued-at dt/sort-latest-first)
                         (into []))
            n       (uix/state 0)
            current (get queued @n)
            ct      (count queued)]
        [:div
         {:class ["flex" "flex-wrap" "place-self-center"
                  "items-center" "space-x-4"
                  "h-full"]}

         [:span
          {:class ["pl-3" "font-mono"]}
          (str (inc @n) "/" ct)]

         [:div
          {:class ["font-mono pr-3" "whitespace-nowrap"]}
          [components.garden/text-with-links (:org/name current)]]

         [components.actions/actions-list
          {:actions
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
             (handlers/->actions current))
           :hide-disabled true}]]))))

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

(defn widget [opts]
  (let [todos         (ui.db/current-todos (:conn opts))
        current-todos (some->> todos (filter todo/current?) seq)]
    [current-stack current-todos]))
