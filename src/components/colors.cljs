(ns components.colors)


(def line-color-classes
  [["text-city-blue-400" "border-city-blue-400"]
   ["text-city-pink-400" "border-city-pink-400"]
   ["text-city-green-400" "border-city-green-400"]
   ["text-city-orange-400" "border-city-orange-400"]
   ["text-slate-400" "border-slate-400"]])

(def bg-color-classes
  [["bg-city-blue-400" "border-city-blue-400"]
   ["bg-city-pink-400" "border-city-pink-400"]
   ["bg-city-green-400" "border-city-green-400"]
   ["bg-city-orange-400" "border-city-orange-400"]
   ["bg-slate-400" "border-slate-400"]])

(defn color-wheel-classes [{:keys [i n type]}]
  (let [wheel
        (cond
          (#{:line} type)
          line-color-classes

          (#{:bg} type)
          bg-color-classes)
        n (or n (count wheel))
        x (mod i (min (count wheel) n))]
    (nth wheel x nil)))

(comment
  (mod 0 5)
  (color-wheel-classes {:type :line :i 0})
  (color-wheel-classes {:type :line :i 4})
  (color-wheel-classes {:type :line :i 3 :n 8})
  (color-wheel-classes {:type :line :i 15 :n 3})
  (color-wheel-classes {:type :line :i 0 :n 2}))
