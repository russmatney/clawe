(ns components.todo
  (:require
   [tick.core :as t]
   [hiccup-icons.fa :as fa]
   [components.floating :as floating]
   [components.garden :as components.garden]
   [components.debug :as components.debug]
   [components.actions :as components.actions]
   [clojure.string :as string]
   [doctor.ui.handlers :as handlers]
   [dates.tick :as dates.tick]
   [uix.core.alpha :as uix]))

(defn status-icon [todo]
  (case (:org/status todo)
    :status/done        fa/check-circle
    :status/not-started fa/sticky-note
    :status/in-progress fa/pencil-alt-solid
    :status/cancelled   fa/ban-solid
    :status/skipped     fa/eject-solid
    (when (:org.prop/archive-time todo)
      [:div.text-sm.font-mono "Archived"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-cell [_opts {:todo/keys [queued-at] :as todo}]
  [:div
   {:class ["grid" "grid-flow-row" "place-items-center"
            "gap-4"
            "border-2"
            "border-slate-600"
            "bg-slate-800"
            "font-mono"
            "w-96"
            "text-center"
            "p-4"]}

   [:div
    [components.garden/text-with-links (:org/name todo)]]

   [:div
    (when (-> todo :org/tags seq)
      [:span
       (for [t (:org/tags todo)]
         ^{:key t}
         [:span {:class ["font-mono"]} (str ":" t ":")])])

    (when (seq (:org/parent-name todo))
      [:div
       [components.garden/text-with-links
        (-> todo :org/parent-name (string/split #" > ") first)]])]

   (when queued-at
     [:div
      (str "queued: " (t/format "MMM d, h:mma"
                                (dates.tick/add-tz
                                  (t/instant
                                    queued-at))))])

   [:div
    {:class ["grid" "grid-flow-col" "gap-2"]}

    [:div
     (status-icon todo)
     #_(:org/status todo)]

    [components.debug/raw-metadata {:label "raw"} todo]

    [floating/popover
     {:hover  true :click true
      :offset 0
      :anchor-comp
      [:div
       {:class ["cursor-pointer"]}
       (:org/short-path todo)]
      :popover-comp
      [:div
       {:class ["p-4"
                "bg-slate-800"
                "border" "border-slate-900"]}
       [components.garden/full-note-popover todo]]}]

    [components.actions/actions-popup (handlers/todo->actions todo)]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label n] :as opts} todos]
  (let [n    (or n (count todos))
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
               :action/on-click (fn [_] (swap! na #(+ % step)))})]}]]]
       [:div {:class ["grid" "grid-flow-row" "grid-cols-3"
                      "gap-4"
                      "place-items-center"]}
        (for [[i td] (->> todos
                          (sort-by :org/parent-name >)
                          (take @na)
                          (map-indexed vector))]
          ^{:key i}
          [:div
           [todo-cell opts td]])]])))
