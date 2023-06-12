(ns timer)

(def start-t (atom (System/currentTimeMillis)))
(def last-t (atom nil))

(defn print-since
  ([] (print-since "Time stamp"))
  ([line]
   (let [now      (System/currentTimeMillis)
         old-last @last-t]
     (reset! last-t now)
     (println "|" (when old-last (- now old-last)) "\t|" (- now @start-t) "\t|" line))))

(print-since "timer ns loaded")
