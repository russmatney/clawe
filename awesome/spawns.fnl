(local awful (require :awful))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spawns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn init_spawns []
  ;; spawn autorun
  (awful.spawn "~/.config/awesome/autorun.sh" false)

  ;; startup some app services
  (awful.spawn "xset r rate 170 60" false)
  (awful.spawn.once "picom" false))

{: init_spawns}
