(ns defthing.defkbd
  (:require
   [clojure.string :as string]
   [defthing.defcom :refer [defcom] :as defcom]
   [defthing.core :as defthing]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Getters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-bindings []
  (defthing/list-things :clawe/binding))

(defn get-binding [bd]
  (defthing/get-thing :clawe/binding
    (comp #{(:name bd bd)} :name)))

(defn binding-cli-command
  "Returns a string that can be called on the command line to execute the
  binding's `defcom` command.

  Consumed when writing these bindings to external configuration files.
  "
  [{:binding/keys [command-name]}]
  (str "clawe " command-name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defkbd
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->key-string
  "Returns a string for the keybinding name + key-def.

  This string is:
  - set on the clawe/binding (at :binding/command-name)
  - used as the symbol/name for the binding's `defcom`
  - used to call the keybinding via `clawe <key-string>` in `binding-cli-command`

  It should have no spaces or other crazy shell-interpreted chars."
  [n key-def]
  (let [mods    (->> (first key-def) (apply str) (#(string/replace % #":" "")))
        key-def (second key-def)]
    (str n "-kbd-" mods key-def)))

(defmacro defkbd
  "Creates both a clawe-binding and a defcom with an associated name.

  Key to this working is the binding's :binding/command-name matching the
  `defcom`'s `:name` (first param).

  The bindings are consumed via `list-bindings` and written to whatever
  key-binding configuration (awesomewm, i3, etc) as shelling out to `clawe` with
  a name determined by `->key-string`.

  The key-def format is a list like: `[<mods> <key>]`
  where `mods` is a list of keywords like:
  - `:mod`, `:alt`, `:shift`, `:ctrl`
  and `key` is a string like:
  - `a`, `b`, `Return`, `Left`, `XF86MonBrightnessUp`

  The rest of the args are passed to `defcom`, which is used to create the
  actual command to be called. `defcom` has it's own docs, but the gist is that
  the final form passed will be adapted into a callable function, so can be
  either an anonymous function, a named function, or a form (like `do` or `let`)
  that will be wrapped as callable function.

  Examples:

  (defkbd say-bye
    [[:mod :ctrl :shift] \"h\"]
    (notify/notify \"Bye!!\"))

  ;; The last expression will be called via defcom, and the keybinding set via sxhkd
  ;; note, the impl for accomplishing this lives in `russmatney/clawe`
  (defkbd open-emacs
    [[:mod :shift] \"Return\"]
    (do
      (notify/notify \"Opening emacs!\")
      (emacs/open)))

  ;; awm/awm-fnl commands get injected into awesome as a pure awm keybinding.
  ;; note, the impl for accomplishing this lives in `russmatney/clawe`
  (defkbd cycle-prev-tag
    [[:mod] \"Left\"]
    (awm/awm-fnl '(awful.tag.viewprev)))
  "
  [n key-def & xorfs]
  (let [full-name (->key-string n key-def)

        ;; pull the defcom fn off the end
        ;; so it isn't called when evaling the binding
        rst (butlast xorfs)

        ;; check for a direct call to `(awm/awm-fnl '(some-fnl-exp))`
        raw-fnl (when (some-> xorfs last first
                              ;; could one day get a better match pattern
                              ;; how to do this from other kbd namespaces?
                              ;; TODO this is a hack, needs to be improved
                              ;; maybe some way to inject these keys or a fn for this macro to consume?
                              #{'awm/awm-fnl})
                  (some-> xorfs last second))

        binding (apply defthing/defthing :clawe/binding n
                       (cond-> {:binding/key          key-def
                                :binding/command-name full-name}

                         raw-fnl (assoc :binding/raw-fnl raw-fnl))
                       rst)]
    `(do
       ;; register a defcom with the full binding name
       ;; may want to only include the fn-form `i.e. (last xorfs)` here,
       ;; if binding defs grow and we don't care for those key-vals on defcoms
       (defcom ~(symbol full-name) ~@xorfs)

       ;; return the binding
       ~binding)))

(comment
  (defkbd say-bye
    [[:mod :ctrl :shift] "h"]
    (println "Bye!!"))

  (->>
    (list-bindings)
    (filter (fn [com] (-> com :name (#(string/includes? % "say-bye")))))
    first
    )

  (->>
    (defthing.defcom/list-commands)
    (filter :name)
    (filter (fn [com] (-> com :name (#(string/includes? % "say-bye")))))
    first
    ;; defthing.defcom/exec
    ))
