(ns clawe.awesome.restart
  (:require [ralph.defcom :refer [defcom]]
            [clawe.awesome.rules :as awm.rules]
            [clawe.awesome.bindings :as awm.bindings]
            [clawe.awesome :as awm]
            [ralphie.notify :as notify]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload Command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom reload-cmd
  {:defcom/name "reload"
   :defcom/handler
   (fn [_ _]
     (notify/notify {:subject "Reloading Clawe"
                     :replaces-process "reloading-clawe"})
     (println "\trewriting awesome rules")
     (awm.rules/write-awesome-rules)

     (println "\trewriting clawe bindings")
     (awm.bindings/write-awesome-bindings)

     ;; (println "\treloading keybindings")
     ;; (awm/reload-keybindings)
     (println "\treloading misc")
     (awm/reload-misc)
     (println "\treloading widgets!")
     (awm/reload-bar-and-widgets))})
