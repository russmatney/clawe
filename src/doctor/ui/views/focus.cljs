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
     {:class
      (concat
        ["flex" "flex-row"]
        (cond
          (completed? it)   ["text-slate-800"]
          (current? it)     ["text-city-pink-300"]
          (not-started? it) []
          :else             ["font-normal"])

        (when (not (or (completed? it) (current? it)))
          (case level
            0 ["text-yo-blue-200"]
            1 ["text-city-blue-dark-300"]
            2 ["text-city-green-400"]
            3 ["text-city-red-200"]
            [])))}

     ;; level ***
     [:span (->> (repeat level "*") (apply str))]

     ;; todo status
     [:span
      {:class ["px-4" "whitespace-nowrap"]}
      (cond
        (completed? it)   "[X]"
        (current? it)     "[-]"
        (not-started? it) "[ ]")]

     ;; name
     [:span
      {:class
       (concat (cond
                 (completed? it)   ["line-through"]
                 (current? it)     ["font-nes"]
                 (not-started? it) []
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
                           (current? it)     ["font-bold"]
                           (not-started? it) []
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
           "font-mono"])
        }
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

      (for [[i it] (->> todos (map-indexed vector))]
        ^{:key i}
        [:div
         [item-header it]
         (when (current? it) [item-body it])])]]))
