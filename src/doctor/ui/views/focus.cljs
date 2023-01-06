(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [clojure.set :as set]
   [clojure.string :as string]
   [uix.core.alpha :as uix]
   [tick.core :as t]
   [dates.tick :as dates]
   [doctor.ui.pomodoros :as pomodoros]
   [components.actions :as components.actions]
   [doctor.ui.handlers :as handlers]
   [components.colors :as colors]))

(defn completed? [it]
  (-> it :org/status #{:status/done}))

(defn current? [it]
  (seq (set/intersection #{"current"} (:org/tags it))))

(defn has-tag? [it tag]
  (seq (set/intersection #{tag} (:org/tags it))))

(defn has-tags? [it tags]
  (seq (set/intersection tags (:org/tags it))))

(defn not-started? [it]
  (-> it :org/status #{:status/not-started}))

(defn skipped? [it]
  (-> it :org/status #{:status/skipped}))

(defn item-name [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    [:span
     {:class
      (concat
        ["flex" "flex-row" "py-2" "justify-center" "items-center"]
        (cond
          (completed? it)   []
          (skipped? it)     []
          (not-started? it) []
          :else             [])

        (case level
          0 ["text-yo-blue-200" "text-xl"]
          1 ["text-city-blue-dark-300" "text-xl"]
          2 ["text-city-green-400" "text-xl"]
          3 ["text-city-red-200" "text-xl"]
          4 ["text-city-pink-300" "text-lg"]
          5 ["text-city-pink-400" "text-lg"]
          6 ["text-city-pink-500" "text-lg"]
          []))}

     ;; level ***
     [:span
      {:class ["px-4" "whitespace-nowrap" "font-nes"]}
      (->> (repeat level "*") (apply str))]

     ;; todo status
     (when (:org/status it)
       [:span
        {:class ["px-4" "whitespace-nowrap" "font-nes"]}
        (cond
          (current? it)     "[-]"
          (completed? it)   "[X]"
          (skipped? it)     "SKIP"
          (not-started? it) "[ ]")])

     ;; name
     [:span
      {:class
       (concat (cond
                 (current? it)     ["font-bold"]
                 (completed? it)   ["line-through"]
                 (not-started? it) []
                 (skipped? it)     ["line-through"]
                 :else             ["font-normal"])
               #_["whitespace-nowrap"])}
      (str (when (:org/priority it)
             (str "#" (:org/priority it) " "))
           (:org/name-string it))]

     ;; time ago
     (when (and (completed? it) (:org/closed-since it))
       [:span
        {:class ["pl-4"
                 "text-3xl"]}
        (str "(" (:org/closed-since it) " ago)")])

     [:span
      {:class ["ml-auto"]}

      ;; tags
      (when (seq (:org/tags it))
        [:span
         {:class
          (concat (cond
                    (completed? it)   []
                    (not-started? it) []
                    (skipped? it)     []
                    (current? it)     ["font-bold"]
                    :else             ["font-normal"])
                  ["pr-4"
                   "text-3xl"
                   "font-nes"
                   ])}

         [:span ":"
          (for [t (:org/tags it)]
            ^{:key t}
            [:span
             ;; TODO popover/tooltip on hover
             {:class    ["cursor-pointer"]
              :on-click (fn [_ev] (use-focus/remove-tag it t))}
             t ":"])]])]]))

(defn item-card [it]
  (let [level     (:org/level it 0)
        level     (if (#{:level/root} level) 0 level)
        hovering? (uix/state nil)]
    [:div
     {:on-mouse-enter (fn [_] (reset! hovering? true))
      :on-mouse-leave (fn [_] (reset! hovering? false))
      :class
      (concat
        ["flex" "flex-col"
         "py-2" "px-3"
         "m-1"
         "bg-yo-blue-700"
         "rounded-lg"
         "w-96"]
        (cond
          (completed? it)   []
          (skipped? it)     []
          (not-started? it) []
          :else             [])

        ["text-city-blue-dark-300" "text-lg"]

        #_(case level
            0 ["text-yo-blue-200"]
            1 ["text-city-blue-dark-300"]
            2 ["text-city-green-400"]
            3 ["text-city-red-200"]
            4 ["text-city-pink-300"]
            5 ["text-city-pink-400"]
            6 ["text-city-pink-500"]
            []))}

     ;; top meta
     [:div
      {:class ["flex" "flex-row" "w-full" "items-center"]}

      ;; level ***
      [:span
       {:class ["whitespace-nowrap" "font-nes"]}
       (->> (repeat level "*") (apply str))]

      ;; todo status
      (when (:org/status it)
        [:span
         {:class ["ml-2" "whitespace-nowrap" "font-nes"]}
         (cond
           (current? it)     "[-]"
           (completed? it)   "[X]"
           (skipped? it)     "SKIP"
           (not-started? it) "[ ]")])

      ;; tags
      (when (seq (:org/tags it))
        [:span
         {:class ["text-md" "font-mono"
                  "px-2"
                  "flex" "flex-row" "flex-wrap"]}
         ":"
         (for [[i t] (->> it :org/tags (map-indexed vector))]
           ^{:key t}
           [:span
            ;; TODO popover/tooltip on hover
            [:span
             {:class
              (concat ["cursor-pointer"
                       "hover:line-through"]
                      (colors/color-wheel-classes {:type :line :i i}))
              :on-click (fn [_ev] (use-focus/remove-tag it t))}
             t]
            ":"])])

      (when (:org/priority it)
        [:span
         {:class
          (concat
            ["whitespace-nowrap" "font-nes" "ml-auto"]
            (let [pri (:org/priority it)]
              (cond
                (#{"A"} pri) ["text-city-red-400"]
                (#{"B"} pri) ["text-city-pink-400"]
                (#{"C"} pri) ["text-city-green-400"])))}
         (str "#" (:org/priority it) " ")])]

     ;; middle content
     [:div
      {:class ["flex" "flex-col" "pb-2"]}

      ;; name
      [:span
       [:span
        {:class
         (concat
           (cond
             (completed? it) ["line-through"]
             (skipped? it)   ["line-through"]))}
        (:org/name-string it)]]

      ;; time ago
      (when (and (completed? it) (:org/closed-since it))
        [:span
         {:class ["font-mono"]}
         (str (:org/closed-since it) " ago")])

      [:span
       (->>
         (:org/parent-names it)
         reverse
         (map-indexed
           (fn [i nm]
             [:span {:class
                     (concat
                       (colors/color-wheel-classes {:type :line :i i})
                       )}
              " " nm]))
         #_reverse
         (interpose [:span " > "]))]]

     ;; bottom meta
     [:div
      {:class ["flex" "flex-row" "text-sm"
               "pb-2"]}

      (when @hovering?
        ;; actions list
        [:span
         {:class ["ml-auto"]}
         [components.actions/actions-list
          {:actions
           (handlers/->actions it (handlers/todo->actions it))
           :nowrap        true
           :hide-disabled true}]])]]))

(defn item-header [it]
  [components.actions/actions-popup
   {:comp [:div {:class
                 (concat
                   ["py-2" "font-mono"])}
           [item-name it]]
    :actions
    (handlers/->actions it (handlers/todo->actions it))}])

(defn item-body [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    (when (-> it :org/body-string seq)
      [:div
       {:class
        (concat
          ["text-xl"
           "text-yo-blue-200"
           "py-4"
           "font-mono"])
        :style {:padding-left (str (* level 25) "px")}}
       [:pre (:org/body-string it)]])))

(defn button [opts label]
  [:button
   (merge
     {:class ["cursor-pointer"
              "bg-city-blue-900"
              "text-city-green-200"
              "text-xl"
              "py-2" "my-2"
              "px-4" "mx-4"
              "rounded"]}
     opts)
   label])

(defn bar [{:keys [time]}]
  [:div
   {:class ["flex flex-row" "items-center"
            "bg-city-blue-700"
            "text-city-green-200"]}
   [:div
    {:class ["text-2xl" "font-nes" "pl-4"]}
    (str
      (t/format
        (t/formatter "h:mma")
        (dates/add-tz time)))]

   (let [p-state                (pomodoros/get-state)
         {:keys [current last]} p-state]
     [:div
      {:class ["ml-auto" "flex" "flex-row"]}

      (when last
        (let [{:keys [started-at finished-at]} last]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Last: " (dates/human-time-since started-at finished-at)]]))

      (when last
        (let [{:keys [finished-at]} last
              latest                (:started-at current)]
          [:div
           {:class ["py-2" "px-4"]}
           [:span
            "Break: " (dates/human-time-since finished-at latest)]]))

      (when current
        (let [{:keys [started-at]} current
              minutes              (t/minutes (dates/duration-since started-at))
              too-long?            (> minutes 40)]
          [:div
           {:class ["py-2" "px-4"]}
           "Current: "
           [:span
            {:class (when too-long? ["text-city-pink-400" "font-nes" "font-bold"
                                     "pl-2" "text-lg" "whitespace-nowrap"])}
            (dates/human-time-since started-at)
            (when too-long? "!!")]]))

      ;; buttons
      (->> (pomodoros/actions)
           (map (fn [{:keys [on-click label]}]
                  [button {:on-click on-click} label]))
           (into [:div]))])])

(defn toggles
  ;; TODO rewrite as actions-based api (duh)
  ;; conditionally hide only-current when there is none
  [{:keys [hide-completed toggle-hide-completed
           only-current toggle-only-current]}]
  [:div
   [button {:on-click (fn [_] (toggle-hide-completed))}
    (if hide-completed "Show completed" "Hide completed")]
   [button {:on-click (fn [_] (toggle-only-current))}
    (if only-current "Show all" "Show only current")]])

(defn widget [opts]
  ;; TODO the 'current' usage in this widget could be a 'tag' based feature
  ;; i.e. based on arbitrary tags, e.g. if that's our mode right now
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data
        current         (some->> todos (filter current?) seq)

        time           (uix/state (t/zoned-date-time))
        interval       (atom nil)
        hide-completed (uix/state nil)
        only-current   (uix/state nil #_(if current true nil))]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 1000))
      (fn [] (js/clearInterval @interval)))

    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"]}

     [bar (assoc opts :time @time)]

     [:div
      {:class ["flex" "flex-row" "items-center" "py-2"]}

      ;; TODO rewrite as actions-based api
      [toggles {:toggle-hide-completed (fn [] (swap! hide-completed not))
                :hide-completed        @hide-completed
                :toggle-only-current   (fn [] (swap! only-current not))
                :only-current          @only-current}]

      (when (and current @only-current)
        [:div
         {:class ["ml-auto"
                  "text-4xl"
                  "text-city-green-200"
                  "px-8"
                  "pb-4"]}
         (->> current
              (mapcat (comp reverse :org/parent-names))
              (into #{})
              (string/join " - "))])]

     ;; TODO group by priority?
     (when (seq todos)
       [:div
        {:class
         ["flex" "flex-row" "flex-wrap" "justify-center"]
         ;; ["grid" "grid-flow-cols" "auto-col-max"]
         ;; ["columns-1" "md:columns-2" "lg:columns-3"]
         }
        (for [[i it] (cond->> todos
                       @hide-completed (remove completed?)
                       @only-current   (filter current?)
                       true            (sort-by (comp (fn [p]
                                                        (if p
                                                          (.charCodeAt p)
                                                          ;; some high val
                                                          1000))
                                                      :org/priority)
                                                <)
                       true            (map-indexed vector))]
          ^{:key i}
          [item-card it])])

     (when
         ;; this could also check commit status, dirty/unpushed commits, etc
         (and (seq todos)
              (->> todos (filter current?) seq not))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no :current: todo!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         "What are ya, taking a load off? GERT BERK TER WORK!"]])
     (when (not (seq todos))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no todos found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         "Did you forget to tag something with :goals: or :focus: ?"]])]))
