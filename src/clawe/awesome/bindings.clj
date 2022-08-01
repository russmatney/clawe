(ns clawe.awesome.bindings
  "Manages awesomeWM bindings derived from clawe in-memory data structures."
  (:require
   [defthing.defkbd :as defkbd]
   [clojure.string :as string]))

(def modifiers {:mod     "Mod4"
                :alt     "Mod1"
                :shift   "Shift"
                :ctrl    "Control"
                :control "Control"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update awm bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn binding->awful-key
  "Maps a clawe keybinding to a call to `awful.key`.

  These bindings are now opt-in, unless you use raw-fnl.
  "
  [{:keys [binding/key binding/raw-fnl binding/awm] :as bd}]

  (let [[mods key] key
        mods       (->> mods
                        (map modifiers)
                        (map #(str "\"" % "\"")))]
    ;; TODO this would read cleaner if it were quoted lisp instead of strings
    (when (or raw-fnl awm)
      (str
        "  (awful.key " "[ " (string/join " " mods) " ] \"" key "\"" "\n    "
        (cond raw-fnl (str "(fn [] " raw-fnl " )")

              ;; these are now opt-in, unless you use raw-fnl
              awm (str "(spawn-fn \"" (defkbd/binding-cli-command bd) "\" )"))
        ")"))))

(comment
  (binding->awful-key
    {:binding/key          [[:mod] "Yah!"]
     ;; :binding/raw-fnl      '(let [] (pp "yah!"))
     :binding/command-name "yah-command"}))

(defn raw-append-global-keybindings
  "Returns a string of fennel to be evaluated."
  []
  ;; TODO this adds a listener for each key, but does not clear any existing,
  ;; still need to restart awesome to prevent bindings from firing multiple
  ;; listeners
  (let [awful-keys (->> (defkbd/list-bindings)
                        (map binding->awful-key)
                        (remove nil?)
                        (string/join "\n"))]
    (str "(awful.keyboard.append_global_keybindings\n[\n" awful-keys "\n])")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write out code that awesome loads at startup/restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def spawn-fn-deps
  "Returns a string of fennel pre-amble to the clawe-bindings.fnl file.
  Should include all fennel deps in bindings.
  ;; TODO generate this based on keywords used in fennel commands?"
  (string/join
    "\n"
    (list '(local gears (require :gears))
          '(local awful (require :awful))
          '(local naughty (require :naughty))
          '(local lume (require :lume))
          '(local spawn-fn-cache {}))))

(def spawn-fn-impl
  '(fn spawn-fn
     [cmd]
     (fn []
       (if (. spawn-fn-cache cmd)
         (do
           (pp cmd)
           (pp spawn-fn-cache)
           (naughty.notify {:title "Dropping binding call"
                            :text  cmd})
           (gears.timer.start_new 0.03
                                  (fn []
                                    (tset spawn-fn-cache cmd nil)
                                    false)))
         (do
           (tset spawn-fn-cache cmd true)
           (awful.spawn.easy_async
             cmd
             (fn [stdout stderr _exitreason _exitcode]
               (tset spawn-fn-cache cmd nil)
               (when stdout (print stdout))
               (when stderr (print stderr)))))))))

(defn write-awesome-bindings []
  (let [str-to-eval   (raw-append-global-keybindings)
        file-contents (str ";; deps helpers\n"
                           spawn-fn-deps
                           "\n\n"
                           spawn-fn-impl
                           "\n;; global bindings\n"
                           "(set _G.append_clawe_bindings\n(fn []\n"
                           str-to-eval
                           "))")
        file-contents (string/replace file-contents "," "")]
    ;; overwrites every time
    (spit "/home/russ/.config/awesome/clawe-bindings.fnl" file-contents)))

(comment
  (->> (defkbd/list-bindings)
       (map binding->awful-key))

  (write-awesome-bindings))
