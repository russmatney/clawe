(ns components.debug
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.floating :as floating]
   [components.format :as format]))

(defn colls-last [m]
  (->> m
       (sort-by (fn [k-v]
                  (and (vector? k-v)
                       (-> k-v second coll?))))))

(defn map-key-sort [m]
  (->> m (sort-by first
                  (fn [a b]
                    (cond (and a b)
                          (> a b)
                          a     a
                          :else b)))
       colls-last))

(comment
  (colls-last {:some-v 5 :some-coll [5]}))


(defui colorized-metadata
  [{:keys [exclude-key level data] :as opts}]
  (let [level       (or level 0)
        exclude-key (or exclude-key #{})]
    (cond
      (map? data)
      (cond->> data
        (not (:no-sort opts)) map-key-sort
        true                  (map #(colorized-metadata (assoc opts :level (inc level) :data %))))

      :else
      (let [[k v] data]
        (when-not (exclude-key k)
          ^{:key k}
          ($ :div.font-mono
             {:class ["text-slate-800" (str "px-" (* 2 level))]}
             "["
             ($ :span {:class ["text-city-pink-400"]} (str k))
             " "

             (cond
               (and (map? v) (seq v))
               (cond->> v
                 (not (:no-sort opts)) map-key-sort
                 true                  (map #(colorized-metadata (assoc opts :level (inc level) :data %))))

               (and (or (list? v)
                        (set? v)
                        (and
                          (seq? v)
                          (not (string? v))))
                    (seq? v)
                    (seq v))
               (->> v
                    (take 10)
                    (map #(colorized-metadata (assoc opts :level (inc level) :data %))))

               :else
               ($ :span {:class ["text-city-green-400" "max-w-xs"]}
                  (cond
                    (and (map? v) (empty? v))  "{}"
                    (and (list? v) (empty? v)) "[]"
                    (nil? v)                   "nil"
                    (false? v)                 "false"
                    :else
                    (when v (format/s-shortener (str v))))))

             "] "))))))


(defui raw-metadata [opts]
  (let [no-sort (:no-sort opts)
        data    (:data opts opts)
        label   (if (= false (:label opts)) nil (:label opts "Toggle raw metadata"))]
    ($ floating/popover
       {:hover true :click true
        :anchor-comp
        ($ :span.text-sm
           {:class ["hover:text-city-pink-400" "cursor-pointer"]}
           label)

        :popover-comp-props {:class ["max-w-7xl"]}
        :popover-comp
        ($ :div
           {:class ["mt-auto" "p-4" "bg-yo-blue-700"
                    "border"
                    "border-city-blue-800"]}
           (when data
             (cond->> data
               (not (map? data)) (take 10)
               (not no-sort)     map-key-sort
               true
               (map #(colorized-metadata (assoc opts :data %)))
               true
               (map-indexed
                 (fn [i x]
                   ;; space between items
                   ($ :div {:key i :class ["pt-2"]} x))))))})))
