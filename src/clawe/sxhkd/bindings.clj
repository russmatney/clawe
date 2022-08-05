(ns clawe.sxhkd.bindings
  "Manages bindings for sxhkd derived from defthing.defkbd."
  (:require
   [defthing.defkbd :as defkbd :refer [defkbd]]
   [clojure.string :as string]
   [babashka.process :as proc]
   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as zsh]))

(defn log [msg]
  ;; TODO consider clawe.log macro
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.sxhkd
       :notify/subject msg
       :notify/body    :clawe.sxhkd})))


(def modifiers {:mod     "super"
                :alt     "alt"
                :shift   "shift"
                :ctrl    "ctrl"
                :control "ctrl"})

(def keyname {"." "period"
              "," "comma"})

(defn sxhkd-exec
  "Basic bb/process wrapper, used to map inlined sxhkd bindings in `binding->sxhkd-key`."
  ;; TODO may need to move to a macro or otherwise eval the string...
  [str]
  (->
    (proc/process str {:out :string})
    proc/check
    :out))

(comment
  (sxhkd-exec "echo 'hi'"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn binding->sxhkd-key
  "Maps a clawe keybinding to a call to `awful.key`."
  [{:keys [binding/key binding/raw-fnl binding/awm binding/body-str] :as bd}]
  (let [[mods key] key
        mods       (->> mods (map modifiers))
        key        (keyname key key)
        cmd-str    (fn [cmd] (str (string/join " + " mods) (when (seq mods) " + ") key "\n  " cmd))
        ]
    ;; TODO rewrite so that the bindings impls aren't aware of each other
    (cond
      (or raw-fnl awm) nil

      (and body-str (re-seq #"sxhkd-exec" body-str))
      (let [res (re-seq #"sxhkd-exec +\"(.*)\"\)$" body-str)
            cmd (some-> res first second)]
        (if cmd
          (cmd-str cmd)
          ;; fallback, just in-case the regex is weird
          (cmd-str (defkbd/binding-cli-command bd))))

      ;; fallback to full-on `clawe <command-name>`
      :else (cmd-str (defkbd/binding-cli-command bd)))))

(comment
  (defkbd test-sxhkd-raw-1
    [[:mod] "Blah"]
    (sxhkd-exec "echo 'hi'"))
  (defkbd test-sxhkd-raw-2
    [[:mod] "Blah"]
    (sxhkd-exec "bb -x clawe.workspace.open/rofi-open-workspace"))

  (binding->sxhkd-key test-sxhkd-raw-1)
  (binding->sxhkd-key test-sxhkd-raw-2)

  (binding->sxhkd-key
    {:binding/key          [[:mod] "Yah!"]
     ;; :binding/raw-fnl      '(let [] (pp "yah!"))
     :binding/command-name "yah-command"}))


(defn raw-sxhkdrc
  "Returns an sxhkdrc config file."
  []
  (->> (defkbd/list-bindings)
       (map binding->sxhkd-key)
       (remove nil?)
       (string/join "\n\n")))

(defn ensure-sxhkd-tmux-session []
  (r.tmux/ensure-session {:tmux/session-name "sxhkd"
                          :tmux/directory    "~"}))

(defn reset-bindings []
  (log "SXHKD bindings resetting...")
  (let [config (raw-sxhkdrc)]
    (spit (zsh/expand "~/.config/sxhkd/sxhkdrc") config)
    (log "SXHKD bindings rewritten.")
    ;; TODO watcher (in doctor?) that just does this?
    (-> (proc/$ systemctl --user restart sxhkd)
        (proc/check))
    (ensure-sxhkd-tmux-session)
    (log "SXHKD bindings reset.")))

(comment
  (reset-bindings)
  )
