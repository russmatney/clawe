(ns clawe.awesome.bindings
  "Manages awesomeWM bindings derived from clawe in-memory data structures."
  (:require
   [clawe.bindings :as bindings]
   [clojure.string :as string]
   [clawe.awesome :as awm]))

(def modifiers {:mod     "Mod4"
                :shift   "Shift"
                :control "Control"})

(defn binding->awful-key
  [{:keys [binding/key defcom/name]}]
  ;; TODO assertion/warning if a binding is missing something here?
  (let [[mods key] key
        mods       (->> mods
                        (map modifiers)
                        (map #(str "\"" % "\"")))]
    (str
      "(awful.key "
      "[ " (string/join " " mods) " ] \"" key "\""
      ;; TODO support wider range of function calls here
      "\n\t\t(spawn-fn \"clawe " name "\" )"
      ")")))

(defn append-global-keybindings []
  ;; TODO this adds a listener for each key, but does not clear any existing...
  (let [awful-keys (->> (bindings/list-bindings)
                        (map binding->awful-key)
                        (string/join "\n\t"))]
    (str "(awful.keyboard.append_global_keybindings [\n\t" awful-keys "\n])")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update the bindings in-place
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; does not work yet - awesome requires holding onto refs to the same `awful.key`
;; to be able to remove things, so for now updating means rewriting the def and
;; then restarting awesome :(
;; probable next steps are moving to a non-awesomewm keybinding handler
(defn update-awesome-bindings []
  ;; TODO might need to remove newlines here or in awm-fnl (or just handle them)
  ;; TODO also might need to ensure deps/requires for these
  (let [global (append-global-keybindings)]
    (awm/awm-fnl global)))

(comment
  (update-awesome-bindings)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write out code that awesome loads at startup/restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def spawn-fn-deps ""
  (str
    "(local gears (require :gears))\n"
    "(local awful (require :awful))\n"
    "(local naughty (require :naughty))\n"
    "(local spawn-fn-cache {:spawn-fn-cache \"\"})\n"))

(def spawn-fn-impl
  '(fn spawn-fn
     [cmd]
     (fn []
       (pp "spawning-fn")
       (pp cmd)
       (if (. spawn-fn-cache cmd)
         (do
           (naughty.notify {:title "Dropping binding call"
                            :text  cmd})
           (gears.timer
             {:timeout  5
              :callback (fn [] (tset spawn-fn-cache cmd nil))}))
         (do
           (tset spawn-fn-cache cmd true)
           (awful.spawn.easy_async
             cmd
             (fn [stdout stderr _exitreason _exitcode]
               (tset spawn-fn-cache cmd nil)
               (when stdout (print stdout))
               (when stderr (print stderr)))))))))

(defn write-awesome-bindings []
  (let [str-to-eval   (append-global-keybindings)
        file-contents (str ";; deps helpers\n"
                           spawn-fn-deps
                           "\n\n"
                           spawn-fn-impl
                           "\n;; global bindings\n"
                           "(set _G.append_clawe_bindings (fn []\n"
                           str-to-eval
                           "))")
        file-contents (string/replace file-contents "," "")]
    ;; overwrites every time
    (spit "/home/russ/.config/awesome/clawe-bindings.fnl" file-contents)))

(comment
  (->> (bindings/list-bindings)
       (map binding->awful-key))

  (write-awesome-bindings))
