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
    :status/skipped     fa/ban-solid
    (when (:org.prop/archive-time todo)
      [:div.text-sm.font-mono "Archived"])))

(defn todo-popover
  [{:keys [on-select]} item]
  (let [{:db/keys   [id]
         :org/keys  [body urls name short-path]
         :todo/keys [last-started-at]} item]
    [:div
     {:class    ["py-2" "px-4"
                 "border" "border-city-blue-600"
                 "bg-yo-blue-700"
                 "text-white"]
      :on-click #(on-select)}

     [:div
      {:class ["grid"]}
      [:div
       {:class ["text-3xl"]}
       [status-icon item]]
      #_[components.actions/actions-list (hooks.todos/->actions item)]]

     [:span
      {:class ["text-xl"]}
      [components.garden/text-with-links name]]

     (when last-started-at
       [:div
        {:class ["font-mono"]}
        (t/instant (t/new-duration last-started-at :millis))])

     [:div
      {:class ["font-mono"]}
      short-path]

     (when id
       [:div
        {:class ["font-mono"]}
        (str "DB ID: " id)])

     (when (seq body)
       [:div
        {:class ["font-mono" "text-city-blue-400"
                 "grid" "grid-flow-col"
                 "p-2"
                 "bg-yo-blue-500"]}
        (for [[i line] (map-indexed vector body)]
          (let [{:keys [text]} line]
            (cond
              (= "" text)
              ^{:key i} [:span {:class ["py-1"]} " "]

              :else
              ^{:key i} [:span [components.garden/text-with-links text]])))])

     (when (seq urls)
       [:div
        {:class ["font-mono" "text-city-blue-400"
                 "grid" "grid-flow-col"
                 "pt-4" "p-2"]}
        (for [[i url] (map-indexed vector
                                   ;; TODO should be ingested as a list
                                   (if (string? urls) [urls] urls))]
          ^{:key i}
          [:a {:class ["py-1"
                       "cursor-pointer"
                       "hover:text-yo-blue-400"]
               :href  url}
           url])])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; line
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line [_opts todo]
  [:div
   {:class ["grid" "grid-flow-col" "space-x-2" "place-items-center"]}

   [status-icon todo]

   [components.garden/text-with-links (:org/name todo)]

   [components.debug/raw-metadata {:label "raw"} todo]])

(comment (line nil nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo-cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-cell [{:keys [index selected on-select]}
                 {:todo/keys [queued-at] :as todo}]
  [:div
   {:class ["grid" "grid-flow-row" "place-items-center"
            "gap-4"
            "border-2"
            "border-slate-600"
            "bg-slate-800"
            "font-mono"
            "w-96"
            "text-center"]}

   [:div
    {:class ["grid" "grid-flow-col" "gap-2"]}

    [:div
     (:org/status todo)]

    [components.debug/raw-metadata {:label "raw"} todo]

    [floating/popover
     {:hover true :click true
      :anchor-comp
      [:div
       {:class ["cursor-pointer"]}
       "show"]
      :popover-comp
      [:div
       {:class []}
       [todo-popover
        {:on-select    #(on-select todo)
         :is-selected? (= selected todo)}
        (assoc todo :index index)]]}]

    [components.actions/actions-popup (handlers/todo->actions todo)]]

   (when queued-at
     [:div
      (str "queued: " (t/format "MMM d, h:mma"
                                (dates.tick/add-tz
                                  (t/instant
                                    queued-at))))])

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
           #_[line nil td]
           [todo-cell (assoc opts :index i) td]])]])))
