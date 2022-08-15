(ns components.todo
  (:require
   [tick.core :as t]
   [hiccup-icons.fa :as fa]
   [components.floating :as floating]
   [components.garden :as components.garden]
   [components.debug :as components.debug]))

(defn status [todo]
  (case (:org/status todo)
    :status/done        fa/check-circle
    :status/not-started fa/sticky-note
    :status/in-progress fa/pencil-alt-solid
    :status/cancelled   fa/ban-solid
    :status/skipped     fa/ban-solid
    (when (:org.prop/archive-time todo)
      [:div.text-sm.font-mono "Archived"])))

(defn todo
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
       [status item]]
      #_[components.actions/action-list (hooks.todos/->actions item)]]

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

   [status todo]

   [components.garden/text-with-links (:org/name todo)]

   [components.debug/raw-metadata {:label "raw"} todo]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label selected on-select n]} todos]
  (let [n (or n (count todos))]
    (when (seq todos)
      [:div {:class ["grid" "grid-flow-row" "place-items-center"]}
       [:div {:class ["text-2xl" "p-2" "pt-4"]} label]
       [:div {:class ["grid" "grid-flow-row"]}
        (for [[i td] (->> todos
                          (sort-by :org/parent-name >)
                          (take n)
                          (map-indexed vector))]
          ^{:key i}
          [floating/popover
           {:hover true
            :click true
            :anchor-comp
            [:div
             {:class ["cursor-pointer"]}
             [line {} td]]
            :popover-comp
            [:div
             {:class []}
             [todo
              {:on-select    #(on-select td)
               :is-selected? (= selected td)}
              (assoc td :index i)]]}])]])))
