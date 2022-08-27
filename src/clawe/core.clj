(ns clawe.core
  (:require
   [defthing.defcom :as defcom]
   clawe.defs.bindings ;; include bindings for `fire` below
   clawe.restart ;; include bindings for `fire` below
   ralphie.core ;; so we include all ralphie commands

   [clojure.string :as string]))

(defn fire [{:keys [command-name]}]
  (defcom/run command-name))

(comment
  (fire {:command-name "uuid-on-clipboard"})
  (fire {:command-name "uuid-on-clipboard-kbd-modctrlu"})
  )

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

(comment
  (->>
    ["hello!"]
    (map #(string/split % #"~")))
  )
