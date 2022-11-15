(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [clojure.set :as set]
   [clojure.string :as string]))

(defn completed? [it]
  (-> it :org/status #{:status/done}))

(defn current? [it]
  (seq (set/intersection #{"current"} (:org/tags it))))

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
        ["flex" "flex-row"]
        (cond
          (completed? it)   ["text-slate-800"]
          (skipped? it)     ["text-slate-800"]
          (not-started? it) []
          :else             ["font-normal"])

        (when (not (or (completed? it) (current? it)))
          (case level
            0 ["text-yo-blue-200"]
            1 ["text-city-blue-dark-300"]
            2 ["text-city-green-400"]
            3 ["text-city-red-200"]
            4 ["text-city-pink-300"]
            5 ["text-city-pink-400"]
            6 ["text-city-pink-500"]
            [])))}

     ;; level ***
     [:span (->> (repeat level "*") (apply str))]

     ;; todo status
     [:span
      {:class ["px-4" "whitespace-nowrap"]}
      (cond
        (completed? it)   "[X]"
        (skipped? it)     "SKIP"
        (not-started? it) "[ ]"
        (current? it)     "[-]")]

     ;; name
     [:span
      {:class
       (concat (cond
                 (completed? it)   ["line-through"]
                 (not-started? it) []
                 (skipped? it)     ["line-through"]
                 (current? it)     ["font-nes"]
                 :else             ["font-normal"])
               #_["whitespace-nowrap"])}
      (:org/name it)]

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
         {:class (concat (cond
                           (completed? it)   []
                           (not-started? it) []
                           (skipped? it)     []
                           (current? it)     ["font-bold"]
                           :else             ["font-normal"])
                         ["pr-4"
                          "text-3xl"])}
         (->> (:org/tags it)
              (string/join ":")
              (#(str ":" % ":")))])]]))

(defn item-header [it]
  [:h1 {:class
        (concat
          ["text-4xl"
           "py-4"
           "font-mono"])}
   (item-name it)])

(defn item-body [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    (when (-> it :org/body-string seq)
      [:div
       {:class
        (concat
          ["text-3xl"
           "text-yo-blue-200"
           "py-4"
           "font-mono"])
        :style {:padding-left (str (* level 25) "px")}}
       [:pre (:org/body-string it)]])))

(defn widget [_opts]
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data]
    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"]}
     [:div {:class ["px-4"]}

      (when (seq todos)
        (for [[i it] (->> todos (map-indexed vector))]
          ^{:key i}
          [:div
           [item-header it]
           (when (current? it) [item-body it])]))

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
          "Did you forget to tag something with :goals: ?"]])]]))
