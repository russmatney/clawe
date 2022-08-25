(ns components.todo
  (:require
   [tick.core :as t]
   [components.floating :as floating]
   [components.garden :as components.garden]
   [components.debug :as components.debug]
   [components.actions :as components.actions]
   [doctor.ui.handlers :as handlers]
   [dates.tick :as dates.tick]
   [uix.core.alpha :as uix]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-cell [_opts {:todo/keys [queued-at] :as todo}]
  [:div
   {:class ["grid" "grid-flow-row"
            "gap-4"
            "border-2"
            "border-slate-600"
            "bg-slate-800"
            "font-mono"
            "w-96"
            "p-4"]}

   [:div
    {:class ["grid" "grid-flow-col"]}
    [components.actions/actions-list
     {:actions (handlers/->actions todo)
      :n       5}]

    [components.debug/raw-metadata {:label [:span
                                            {:class ["text-slate-400"]}
                                            "raw"]} todo]]

   [:div
    {:class ["inline-flex" "items-center"]}
    [:div
     {:class ["text-slate-400" "pr-2"]}
     (components.garden/status-icon todo)]

    [components.garden/text-with-links (:org/name todo)]]

   [components.garden/tags-comp todo]

   (when queued-at
     [:div
      (str "Q'd: " (t/format "MMM d, h:mma"
                             (dates.tick/add-tz
                               (t/instant
                                 queued-at))))])

   [floating/popover
    {:hover  true :click true
     :offset 0
     :anchor-comp
     [:div
      {:class ["cursor-pointer" "text-slate-400"]}
      (when (seq (:org/parent-name todo))
        [:div
         [components.garden/text-with-links
          (str
            (-> todo :org/parent-name)
            " :: "
            (:org/short-path todo))]])]
     :popover-comp
     [:div
      {:class ["p-4"
               "bg-slate-800"
               "border" "border-slate-900"]}
      [components.garden/full-note todo]]}]])

(defn todo-row [opts {:todo/keys [queued-at] :as todo}]
  [:div
   {:class ["grid" "grid-flow-col" "grid-cols-8"
            "gap-4"
            "border"
            "border-slate-600"
            "bg-slate-800"
            "font-mono"
            "p-4"]}

   [:div
    {:class ["inline-flex" "items-center" "col-span-4"]}
    [:div
     {:class ["text-slate-400" "pr-4"]}
     [components.debug/raw-metadata
      {:label [:span
               {:class ["text-slate-400"]}
               (components.garden/status-icon todo)]} todo]]

    [components.garden/text-with-links (:org/name todo)]]

   (when queued-at
     [:div
      {:class ["text-slate-400"]}
      (t/format "E ha" (dates.tick/add-tz (t/instant queued-at)))])

   [:div
    {:class ["justify-self-end"]}
    [components.garden/tags-comp todo]

    (for [tag (components.garden/all-tags opts todo)]
      (do
        (println "tag?" tag)
        [:div
         {:key   tag
          :class [""]}
         tag]))]

   [:div
    {:class ["justify-self-end"]}
    [floating/popover
     {:hover  true :click true
      :offset 0
      :anchor-comp
      [:div
       {:class ["cursor-pointer" "text-slate-400"
                ]}
       (when (seq (:org/parent-name todo))
         [:div
          [components.garden/text-with-links
           (str
             (-> todo :org/parent-name)
             " :: "
             (:org/short-path todo))]])]
      :popover-comp
      [:div
       {:class ["p-4"
                "bg-slate-800"
                "border" "border-slate-900"]}
       [components.garden/full-note todo]]}]]

   [:div
    {:class ["justify-self-end" "col-span-2"]}
    [components.actions/actions-list
     {:actions (handlers/->actions todo)
      :n       5}]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label n] :as opts} todos]
  (let [n    (or n (count todos))
        n    (if (> n (count todos)) (count todos) n)
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
       [:div {:class ["grid" "grid-flow-row"
                      "w-full"
                      "px-4"]}
        (for [[i td] (->> todos
                          (sort-by :org/parent-name >)
                          (take @na)
                          (map-indexed vector))]
          ^{:key i}
          [todo-row opts td])]])))
