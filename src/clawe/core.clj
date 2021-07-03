(ns clawe.core
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [clawe.awesome :as awm]
   clawe.awesome.rules
   clawe.defs.bindings
   clawe.defs.workspaces
   clawe.defs.local.workspaces
   clawe.doctor
   clawe.dwim
   clawe.install
   clawe.restart
   clawe.rules
   clawe.workrave
   clawe.workspaces
   clawe.workspaces.create
   ralphie.core ;; so we include all ralphie commands
   [ralphie.rofi :as r.rofi]))

(defcom hello-cmd (println "Howdy"))

(defcom rofi
  (when-let [cmd (some->> (defcom/list-commands)
                          (map :name)
                          (r.rofi/rofi {:require-match? true
                                        :msg            "Clawe commands"}))]
    (defcom/exec cmd)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awm-cli wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
