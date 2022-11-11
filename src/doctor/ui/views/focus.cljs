(ns doctor.ui.views.focus
  (:require
   [doctor.ui.hooks.use-focus :as use-focus]
   [clojure.set :as set]))

(defn completed? [it]
  (-> it :org/status #{:status/done}))

(defn current? [it]
  (seq (set/intersection #{"current"} (:org/tags it))))

(defn not-started? [it]
  (-> it :org/status #{:status/not-started}))

(defn item-name [it]
  [:span
   [:span
    {:class ["pr-4"]
     :style {:padding-left
             (let [level (:org/level it 0)
                   level (if (#{:level/root} level) 0 level)]
               (str (* 12 level) "px"))}}
    (cond
      (completed? it)   "[X]"
      (current? it)     "[-]"
      (not-started? it) "[ ]")]
   (:org/name it)])

(defn item-header [it]
  [:h1 {:class
        (concat
          ["text-3xl"
           "font-mono"
           "py-4"]
          (cond
            (completed? it)   ["text-slate-800"
                               "line-through"]
            (current? it)     ["text-city-green-400"
                               "text-weight-600"
                               "text-4xl"
                               ]
            (not-started? it) ["text-city-pink-300"]
            :else             ["text-yo-blue-200"]))}
   (item-name it)])

(defn widget [_opts]
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data]
    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-60"
              "h-screen"]}
     [:div
      {:class ["bg-city-blue-800"
               "bg-opacity-60"
               "h-full"
               "flex" "flex-col"]}
      [:div {:class ["px-4"]}

       (for [[i it] (->> todos (map-indexed vector))]
         ^{:key i}
         [item-header it])]]]))
