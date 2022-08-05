(ns clawe.core
  (:require
   [defthing.defcom :as defcom]
   clawe.awesome.rules
   clawe.client
   clawe.defs.bindings
   clawe.doctor
   clawe.toggle
   clawe.restart
   clawe.rules
   clawe.m-x
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
