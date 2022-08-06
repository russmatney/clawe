(ns clawe.core
  (:require
   [defthing.defcom :as defcom]
   clawe.defs.bindings
   clawe.restart
   ralphie.core ;; so we include all ralphie commands
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (let [start-time (System/currentTimeMillis)
        _          (println "[CLAWE] start" (str "[" start-time "]") args)
        res        (apply defcom/run args)
        end-time   (System/currentTimeMillis)
        dt         (- end-time start-time)]
    (println "[CLAWE] complete" args "in" dt "ms" (str "[" end-time "]"))
    res))
