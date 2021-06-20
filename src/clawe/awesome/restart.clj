(ns clawe.awesome.restart
  (:require
   [defthing.defcom :refer [defcom]]
   [clawe.awesome.rules :as awm.rules]
   [clawe.awesome.bindings :as awm.bindings]
   [clawe.awesome :as awm]
   [ralphie.notify :as notify]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload Command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move to top-level restart/reload ns?
(defcom reload
  (do
    (notify/notify {:subject          "Reloading Clawe"
                    :replaces-process "reloading-clawe"})
    (println "\trewriting awesome rules")
    (awm.rules/write-awesome-rules)

    (println "\trewriting clawe bindings")
    (awm.bindings/write-awesome-bindings)

    (println "\tapplying clawe rules")
    (awm.rules/apply-rules)

    ;; (println "\treloading keybindings")
    ;; (awm/reload-keybindings)
    (println "\treloading misc")
    (awm/reload-misc)
    (println "\treloading widgets!")
    (awm/reload-bar-and-widgets)))
