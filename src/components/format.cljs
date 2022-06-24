(ns components.format)

(defn s-shortener
  ([s] (s-shortener nil s))
  ([opts s]
   (let [total-len (:length opts 30)
         half-len  (/ total-len 2)]
     (if (< (count s) total-len)
       s
       (let [start (take half-len s)
             end   (->> s reverse (take half-len) reverse)]
         (apply str (concat start "..." end)))))))

(comment
  (s-shortener "some really long string with lots of thoughts that never end")
  (s-shortener
    {:length 20}
    "some really long string with lots of thoughts that never end")
  )
