(ns clawe.awesome.restart
  (:require [ralph.defcom :refer [defcom]]
            [clawe.awesome.rules :as awm.rules]
            [clawe.awesome :as awm]
            [ralphie.notify :as notify]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload Command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom reload-cmd
  {:defcom/name "reload"
   :defcom/handler
   (fn [_ _]
     (notify/notify "Reloading Clawe")
     (println "\trewriting awesome rules")
     (awm.rules/write-awesome-rules)
     (println "\treloading keybindings")
     (awm/reload-keybindings)
     (println "\treloading misc")
     (awm/reload-misc)
     (println "\treloading widgets!")
     (awm/reload-bar-and-widgets))})
