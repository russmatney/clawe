(ns components.format
  (:require
   [tick.core :as t]))

(defn s-shortener
  ([s] (s-shortener nil s))
  ([opts s]
   (let [total-len (:length opts 30)
         break     (:break opts "...")
         half-len  (/ total-len 2)]
     (if (<= (+ (count s)) (+ (count break) total-len))
       s
       (let [start (take half-len s)
             end   (->> s reverse (take half-len) reverse)]
         (apply str (concat start
                            break
                            end)))))))

(comment
  (s-shortener {:length 7} "spotify")

  (s-shortener "some really long string with lots of thoughts that never end")
  (s-shortener
    {:length 20}
    "some really long string with lots of thoughts that never end"))

(defn last-modified-human [last-modified]
  (let [time-ago  (t/duration {:tick/beginning (t/instant last-modified)
                               :tick/end       (t/now)})
        mins-ago  (t/minutes time-ago)
        hours-ago (t/hours time-ago)
        days-ago  (t/days time-ago)]
    (cond
      (< mins-ago 60)  (str mins-ago " min(s) ago")
      (< hours-ago 24) (str hours-ago " hour(s) ago")
      :else            (str days-ago " day(s) ago"))))
