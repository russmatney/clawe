(ns components.todo
  (:require
   [tick.core :as t]
   [hiccup-icons.fa :as fa]
   [components.actions]
   [components.floating :as floating]
   [hooks.todos]))

(defn status [todo]
  (case (:todo/status todo)
    :status/done        fa/check-circle
    :status/not-started fa/sticky-note
    :status/in-progress fa/pencil-alt-solid
    :status/cancelled   fa/ban-solid
    :status/skipped     fa/ban-solid
    (when (:org.prop/archive-time todo)
      [:div.text-sm.font-mono "Archived"])))

(defn todo
  [{:keys [on-select]} item]
  (let [{:db/keys       [id]
         :org/keys      [body urls]
         :org.prop/keys [archive-time]
         :todo/keys     [name file-name last-started-at]} item]
    [:div
     {:class    ["py-2" "px-4"
                 "border" "border-city-blue-600"
                 "bg-yo-blue-700"
                 "text-white"]
      :on-click #(on-select)}

     [:div
      {:class ["flex" "justify-between"]}
      [:div
       {:class ["text-3xl"]}
       [status item]]
      [components.actions/action-list (hooks.todos/->actions item)]]

     [:span
      {:class ["text-xl"]}
      name]

     (when last-started-at
       [:div
        {:class ["font-mono"]}
        (t/instant (t/new-duration last-started-at :millis))])

     [:div
      {:class ["font-mono"]}
      file-name]

     (when id
       [:div
        {:class ["font-mono"]}
        (str "DB ID: " id)])

     (when (seq body)
       [:div
        {:class ["font-mono" "text-city-blue-400"
                 "flex" "flex-col" "p-2"
                 "bg-yo-blue-500"]}
        (for [[i line] (map-indexed vector body)]
          (let [{:keys [text]} line]
            (cond
              (= "" text)
              ^{:key i} [:span {:class ["py-1"]} " "]

              :else
              ^{:key i} [:span text])))])

     (when (seq urls)
       [:div
        {:class ["font-mono" "text-city-blue-400"
                 "flex" "flex-col" "pt-4" "p-2"]}
        (for [[i url] (map-indexed vector urls)]
          ^{:key i}
          [:a {:class ["py-1"
                       "cursor-pointer"
                       "hover:text-yo-blue-400"]
               :href  url}
           url])])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; line
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line [opts todo]
  [:div
   {:class ["flex" "flex-row" "items-center"]}

   [:div
    {:class ["p-2"]}
    [status todo]]

   [:div
    (:todo/name todo)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-list [{:keys [label selected on-select]} todos]
  (when (seq todos)
    [:div {:class ["flex" "flex-col"]}
     [:div {:class ["text-2xl" "p-2" "pt-4"]} label]
     [:div {:class ["flex" "flex-col" "justify-center"]}
      (for [[i td] (->> todos (map-indexed vector))]
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
            (assoc td :index i)]]}])]]))
