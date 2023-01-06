(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [clojure.set :as set]
   [uix.core.alpha :as uix]
   [tick.core :as t]
   [dates.tick :as dates]
   [doctor.ui.pomodoros :as pomodoros]
   [components.actions :as components.actions]
   [doctor.ui.handlers :as handlers]
   [components.colors :as colors]
   [components.garden :as components.garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; preds

(defn completed? [it]
  (-> it :org/status #{:status/done}))

(defn not-started? [it]
  (-> it :org/status #{:status/not-started}))

(defn skipped? [it]
  (-> it :org/status #{:status/skipped}))

(defn in-progress? [it]
  (-> it :org/status #{:status/in-progress}))

(defn current? [it]
  (seq (set/intersection #{"current"} (:org/tags it))))

(defn has-tag? [it tag]
  (seq (set/intersection #{tag} (:org/tags it))))

(defn has-tags? [it tags]
  (seq (set/intersection tags (:org/tags it))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; level

(defn level [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    [:div
     {:class ["whitespace-nowrap" "font-nes"]}
     (->> (repeat level "*") (apply str))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo status

(defn todo-status [it]
  (when (:org/status it)
    (let [hovering? (uix/state nil)]
      [:span
       {:class          ["ml-2" "whitespace-nowrap" "font-nes"
                         "cursor-pointer"
                         "hover:opacity-50"]
        :on-mouse-enter (fn [_] (reset! hovering? true))
        :on-mouse-leave (fn [_] (reset! hovering? false))
        :on-click       (fn [_]
                          (handlers/update-todo
                            it (let [status (cond
                                              (completed? it)   :status/not-started
                                              (skipped? it)     :status/not-started
                                              (current? it)     :status/done
                                              (in-progress? it) :status/done
                                              (not-started? it) :status/in-progress)]
                                 (cond-> {:org/status status}
                                   (#{:status/in-progress} status) (assoc :org/tags "current")
                                   (#{:status/done} status)        (assoc :org/tags [:remove "current"])))))}
       (cond
         (and (completed? it) (not @hovering?))   "[X]"
         (and (completed? it) @hovering?)         "[ ]"
         (and (skipped? it) (not @hovering?))     "SKIP"
         (and (skipped? it) @hovering?)           "[ ]"
         (and (current? it) (not @hovering?))     "[-]"
         (and (current? it) @hovering?)           "[X]"
         (and (in-progress? it) (not @hovering?)) "[-]"
         (and (in-progress? it) @hovering?)       "[X]"
         (and (not-started? it) (not @hovering?)) "[ ]"
         (and (not-started? it) @hovering?)       "[-]")])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags-list

(defn tags-list [it]
  (when (seq (:org/tags it))
    [:span
     {:class ["text-md" "font-mono"
              "px-2"
              "flex" "flex-row" "flex-wrap"]}
     ":"
     (for [[i t] (->> it :org/tags (map-indexed vector))]
       ^{:key t}
       [:span
        [:span
         {:class
          (concat ["cursor-pointer" "hover:line-through"]
                  (colors/color-wheel-classes {:type :line :i (+ 2 i)}))
          :on-click (fn [_ev] (use-focus/remove-tag it t))}
         t]
        ":"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; priority-label

(defn priority-label [it]
  (when (:org/priority it)
    [:span
     {:class
      (concat
        ["whitespace-nowrap" "font-nes"]
        (let [pri (:org/priority it)]
          (cond
            (completed? it) ["text-city-blue-dark-400"]
            (skipped? it)   ["text-city-blue-dark-400"]
            (#{"A"} pri)    ["text-city-red-400"]
            (#{"B"} pri)    ["text-city-pink-400"]
            (#{"C"} pri)    ["text-city-green-400"])))}
     (str "#" (:org/priority it) " ")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parent-names

(defn parent-names [it]
  [:span
   (->>
     (:org/parent-names it)
     reverse
     (map-indexed
       (fn [i nm]
         ^{:key i}
         [:span {:class
                 (concat
                   (colors/color-wheel-classes {:type :line :i i}))}
          " " nm]))
     #_reverse
     (interpose [:span
                 {:class ["text-city-blue-dark-200"]}
                 " > "]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defn item-header [it]
  [:div
   {:class ["flex" "flex-col" "px-4"
            "text-city-green-400" "text-xl"]}

   [:div {:class ["flex" "flex-row"]}
    [level it]
    [todo-status it]
    [:div {:class ["ml-auto"]}
     [tags-list it]]
    [priority-label it]]

   [:div {:class ["flex" "flex-row"]}
    [:div {:class ["font-nes"]}
     (:org/name-string it)]]])

(defn item-body [it]
  (when (-> it :org/body-string seq)
    [:div
     {:class ["text-xl" "p-4" "flex" "flex-col"]}
     [:div
      {:class ["text-yo-blue-200" "font-mono"]}
      ;; TODO include sub todos
      #_[:pre (:org/body-string it)]
      [components.garden/org-body it]]

     [:div {:class ["py-4" "flex" "flex-row" "justify-between"]}
      [parent-names it]
      [components.actions/actions-list
       {:actions
        (handlers/->actions it (handlers/todo->actions it))
        :nowrap        true
        :hide-disabled true}]]]))

(defn item-card [it]
  [:div
   {:class
    (concat
      ["flex" "flex-col"
       "py-2" "px-3"
       "m-1"
       "rounded-lg"
       "w-96"
       "text-lg"
       "bg-yo-blue-700"]
      (cond
        (completed? it) ["text-city-blue-dark-400"]
        (skipped? it)   ["text-city-blue-dark-600"]
        ;; (not-started? it) []
        :else           ["text-city-blue-dark-200"]))}

   ;; top meta
   [:div
    {:class ["flex" "flex-row" "w-full" "items-center"]}

    [level it]
    [todo-status it]
    [:div {:class ["ml-auto"]}
     [tags-list it]]
    [priority-label it]]

   ;; middle content
   [:div
    {:class ["flex" "flex-col" "pb-2"]}

    ;; name
    [:span
     [:span
      {:class (when (or (completed? it) (skipped? it)) ["line-through"])}
      (:org/name-string it)]]

    ;; time ago
    (when (and (completed? it) (:org/closed-since it))
      [:span
       {:class ["font-mono"]}
       (str (:org/closed-since it) " ago")])

    [parent-names it]]

   ;; bottom meta
   [:div
    {:class ["flex" "flex-row" "text-sm"
             "mt-auto"
             "pb-2"]}

    ;; actions list
    [:span
     {:class ["ml-auto"]}
     [components.actions/actions-list
      {:actions
       (handlers/->actions it (handlers/todo->actions it))
       :nowrap        true
       :hide-disabled true}]]]])

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

(defn sort-by-priority [its]
  (->> its
       (sort-by (comp (fn [p]
                        (if p
                          (.charCodeAt p)
                          ;; some high val
                          1000))
                      :org/priority)
                <)))

(defn widget [opts]
  ;; TODO the 'current' usage in this widget could be a 'tag' based feature
  ;; i.e. based on arbitrary tags, e.g. if that's our 'mode' right now
  ;; i.e. 'current' is an execution mode - another mode might be pre or post execution
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
                :only-current          @only-current}]]

     (when current
       (for [[i c] (->> current (sort-by-priority) (map-indexed vector))]
         ^{:key i}
         [:div
          {:class ["bg-city-blue-800"]}
          [:hr {:class ["border-city-blue-900" "pb-4"]}]
          [item-header c]
          [item-body c]]))

     ;; TODO group by priority?
     (when (seq todos)
       [:div
        {:class
         (concat
           ["pt-6"]
           ["flex" "flex-row" "flex-wrap" "justify-around"]
           ;; ["grid" "grid-flow-cols" "auto-col-max"]
           ;; ["columns-1" "md:columns-2" "lg:columns-3"]
           )
         }
        (for [[i it] (cond->> todos
                       @hide-completed (remove completed?)
                       @only-current   (filter current?)
                       true            sort-by-priority
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
         ""]])]))
