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
   [dates.tick :as dates.tick]))

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
    {:class ["grid" "grid-flow-col" "gap-2"]}

    [:div
     (status-icon todo)
     #_(:org/status todo)]

    [components.debug/raw-metadata {:label "raw"} todo]

    [floating/popover
     {:hover true :click true
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

    [components.actions/actions-popup (handlers/todo->actions todo)]]

   (when queued-at
     [:div
      (str "queued: " (t/format "MMM d, h:mma"
                                (dates.tick/add-tz
                                  (t/instant
                                    queued-at))))])

   (when (-> todo :org/tags seq)
     [:span
      (for [t (:org/tags todo)]
        ^{:key t}
        [:span {:class ["font-mono"]} (str ":" t ":")])])

   [:div
    [components.garden/text-with-links (:org/name todo)]]

   (when (seq (:org/parent-name todo))
     [:div
      [components.garden/text-with-links
       (-> todo :org/parent-name (string/split #" > ") first)]])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label n] :as opts} todos]
  (let [n (or n (count todos))]
    (when (seq todos)
      [:div {:class ["grid" "grid-flow-row" "place-items-center"]}
       [:div {:class ["text-2xl" "p-2" "pt-4"]} label]
       [:div {:class ["grid" "grid-flow-row" "grid-cols-3"
                      "gap-4"
                      "place-items-center"]}
        (for [[i td] (->> todos
                          (sort-by :org/parent-name >)
                          (take n)
                          (map-indexed vector))]
          ^{:key i}
          [:div
           [todo-cell opts td]])]])))
