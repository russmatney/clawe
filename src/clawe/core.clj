(ns clawe.core
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [ralphie.awesome :as awm]
   clawe.awesome.rules
   clawe.defs.bindings
   clawe.defs.workspaces
   clawe.doctor
   clawe.install
   clawe.toggle
   clawe.restart
   clawe.rules
   clawe.m-x
   clawe.workspaces
   clawe.workspaces.create
   ralphie.core ;; so we include all ralphie commands
   [malli.transform :as mt]
   [malli.core :as m]
   ))

(def simple-schema
  [:map
   [:some/string string?]
   [:some/keyword keyword?]
   [:some/int int?]])

(println
  "malli decode!"
  (m/decode
    simple-schema
    {:some/string    "hi"
     :some/keyword   :yo
     :some/int       5
     :something/else "blah"}
    (mt/strip-extra-keys-transformer)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awm-cli wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move to ralphie.awesome or pure clojure-awesome library/adapter
(defcom awm-cli
  (fn [_config & arguments]
    (let [res (awm/awm-cli (-> arguments first first))]
      (println res))))

(defcom awm-collect-garbage
  (awm/awm-cli "handle_garbage();"))

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
