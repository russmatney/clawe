(ns components.colors)


(def line-color-classes
  [["text-city-blue-400" "border-city-blue-300"]
   ["text-city-pink-400" "border-city-pink-300"]
   ["text-city-green-400" "border-city-green-300"]
   ["text-city-orange-400" "border-city-orange-300"]
   ["text-city-red-400" "border-city-red-300"]
   ["text-slate-400" "border-slate-300"]])

(def bg-color-classes
  [["bg-city-blue-800" "border-city-blue-300"]
   ["bg-city-pink-800" "border-city-pink-300"]
   ["bg-city-green-800" "border-city-green-300"]
   ["bg-city-orange-800" "border-city-orange-300"]
   ["bg-city-red-800" "border-city-red-300"]
   ["bg-slate-800" "border-slate-300"]])

(def hover-line-color-classes
  [["hover:text-city-blue-400" "hover:text-city-blue-300"]
   ["hover:text-city-pink-400" "hover:text-city-pink-300"]
   ["hover:text-city-green-400" "hover:text-city-green-300"]
   ["hover:text-city-orange-400" "hover:text-city-orange-300"]
   ["hover:text-city-red-400" "hover:text-city-red-300"]
   ["hover:text-slate-400" "hover:text-slate-300"]])

(def hover-bg-color-classes
  [["hover:bg-city-blue-800" "hover:border-city-blue-300"]
   ["hover:bg-city-pink-800" "hover:border-city-pink-300"]
   ["hover:bg-city-green-800" "hover:border-city-green-300"]
   ["hover:bg-city-orange-800" "hover:border-city-orange-300"]
   ["hover:bg-city-red-800" "hover:border-city-red-300"]
   ["hover:bg-slate-800" "hover:border-slate-300"]])

(defn color-wheel-classes [{:keys [i n type hover?] :as opts}]
  (let [lines (if hover? hover-line-color-classes line-color-classes)
        bgs   (if hover? hover-bg-color-classes bg-color-classes)
        bgs   (concat (rest bgs) [(first bgs)])]
    (if (#{:both} type)
      (concat (color-wheel-classes (assoc opts :type :line))
              (color-wheel-classes (assoc opts :type :bg)))
      (let [wheel (cond
                    (#{:line} type) lines
                    (#{:bg} type)   bgs)
            n     (or n (count wheel))
            x     (mod i (min (count wheel) n))]
        (nth wheel x nil)))))

(comment
  (mod 0 5)
  (color-wheel-classes {:type :line :i 0})
  (color-wheel-classes {:type :line :i 4})
  (color-wheel-classes {:type :line :i 3 :n 8})
  (color-wheel-classes {:type :line :i 15 :n 3})
  (color-wheel-classes {:type :line :i 0 :n 2}))
