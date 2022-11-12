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

(defn item-name [it]
  (let [level (:org/level it 0)
        level (if (#{:level/root} level) 0 level)]
    [:span
     {:class ["flex" "flex-row"]}
     [:span (->> (repeat level "*") (apply str))]
     [:span
      {:class ["px-4" "whitespace-nowrap"]}
      (cond
        (completed? it)   "[X]"
        (current? it)     "[-]"
        (not-started? it) "[ ]")]
     [:span
      {:class
       (concat (cond
                 (completed? it)   ["line-through"]
                 (current? it)     ["font-bold"
                                    "font-nes"]
                 (not-started? it) []
                 :else             ["font-normal"])
               ["whitespace-nowrap"])}
      (:org/name it)]
     (when (seq (:org/tags it))
       [:span
        {:class (concat (cond
                          (completed? it)   ["line-through"]
                          (current? it)     ["font-bold"]
                          (not-started? it) []
                          :else             ["font-normal"])
                        ["pl-4"])}
        (->> (:org/tags it)
             (string/join ":")
             (#(str ":" % ":")))])]))

(defn item-header [it]
  [:h1 {:class
        (concat
          ["text-4xl"
           "py-4"
           "font-mono"]
          (cond
            (completed? it)   ["text-slate-800"
                               "line-through"]
            (current? it)     ["text-city-green-400"
                               "font-bold"]
            (not-started? it) ["text-city-pink-300"]
            :else             ["text-yo-blue-200"
                               "font-normal"]))}
   (item-name it)])

(defn widget [_opts]
  (let [focus-data      (use-focus/use-focus-data)
        {:keys [todos]} @focus-data]
    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"]}
     [:div {:class ["px-4"]}

      (for [[i it] (->> todos (map-indexed vector))]
        ^{:key i}
        [item-header it])]]))
