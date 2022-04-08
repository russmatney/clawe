(ns clawe.core
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [ralphie.awesome :as awm]
   clawe.awesome.rules
   clawe.defs.bindings
   clawe.defs.workspaces
   clawe.doctor
   clawe.dwim
   clawe.godot
   clawe.install
   clawe.toggle
   clawe.restart
   clawe.rules
   clawe.m-x
   clawe.workrave
   clawe.workspaces
   clawe.workspaces.create
   ralphie.core ;; so we include all ralphie commands
   [ralphie.rofi :as r.rofi]
   ))

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
  (println "[CLAWE] start" args)
  (let [start-time (System/currentTimeMillis)
        res        (apply defcom/run args)
        dt         (- (System/currentTimeMillis) start-time)]
    (println "[CLAWE] complete" args "in" dt "ms")
    res))
