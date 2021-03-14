;; deps helpers
(local gears (require :gears))
(local awful (require :awful))
(local naughty (require :naughty))
(local spawn-fn-cache {:spawn-fn-cache ""})


(fn spawn-fn [cmd] (fn [] (pp "spawning-fn") (pp cmd) (if (. spawn-fn-cache cmd) (do (naughty.notify {:title "Dropping binding call" :text cmd}) (gears.timer {:timeout 5 :callback (fn [] (tset spawn-fn-cache cmd nil))})) (do (tset spawn-fn-cache cmd true) (awful.spawn.easy_async cmd (fn [stdout stderr _exitreason _exitcode] (tset spawn-fn-cache cmd nil) (when stdout (print stdout)) (when stderr (print stderr))))))))
;; global bindings
(set _G.append_clawe_bindings (fn []
(awful.keyboard.append_global_keybindings [
	(awful.key [ "Mod4" ] "v"
		(spawn-fn "clawe toggle-floating-keybdg-modv" ))
	(awful.key [ "Mod4" "Shift" ] "t"
		(spawn-fn "clawe toggle-all-titlebars-keybdg-modshiftt" ))
])))