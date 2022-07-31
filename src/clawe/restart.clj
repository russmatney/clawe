(ns clawe.restart
  (:require
   [babashka.process :as proc]
   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]
   [ralphie.notify :as notify]

   [clawe.install :as c.install]
   [clawe.rules :as c.rules]
   [clawe.workspaces :as workspaces]
   [clawe.awesome.rules :as awm.rules]
   [clawe.awesome.bindings :as awm.bindings]
   [clawe.sxhkd.bindings :as sxhkd.bindings]
   [ralphie.zsh :as zsh]
   [ralphie.tmux :as r.tmux]
   [ralphie.emacs :as r.emacs]
   [clawe.doctor :as clawe.doctor]))


(defn log [msg]
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.restart
       :notify/subject msg
       :notify/body    :clawe.restart})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; check if unit tests are passing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-unit-tests
  "Run the unit tests, return [:success ...] or [:fail ....]."
  []
  (let [proc               ^{:out :string
                             ;; TODO use clawe-dir (config?)
                             :dir (zsh/expand "~/russmatney/clawe")
                             } (proc/$ bb test-unit)
        {:keys [exit out]} @proc]

    (if (#{0} exit)
      (do
        (notify/notify "Unit Test PASS :)")
        [:success out])
      (do
        (notify/notify "Unit Test FAIL :(")
        (notify/notify out)
        (notify/notify exit)
        [:fail out exit]))))

(defn attempt-uberjar-rebuild
  "Runs the unit tests - if they pass, rebuilds the uber jar."
  []
  (let [res (-> (check-unit-tests) first)]

    (cond
      (#{:fail} res)
      (log "Unit tests failed, skipping uberjar rebuild")

      (#{:success} res)
      (do
        ;; TODO detect if the current uberjar is out of date
        ;; maybe using git status, or some local timestamp?
        ;; TODO provide a force rebuild option
        (log "Rebuilding uberjar...")
        ;; TODO could also skip if nothing has changed
        (try
          (c.install/build-uberjar)
          (catch Exception e
            (notify/notify "Exception while rebuilding uberjar")
            (println e))))

      :else
      (log "Strange unit tests response..."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom restart
  "Maybe rebuild the clawe uberjar, then call `reload`.

Rebuild requires the unit tests to pass, but if they fail, `reload` is called anyway.

The uberjar can be manually rebuilt on the command line with:

bb -cp $(clojure -Spath) --uberjar clawe.jar -m clawe.core # rebuild clawe

See `clawe.install/build-uberjar`.
"
  (do
    (log "Restarting...")
    (attempt-uberjar-rebuild)

    ;; reload afterwards, pretty much no matter what
    (log "reloading clawe...")
    (->
      ;; kicking to process here so it can use the new uberjar
      ;; Also helps avoid a circular dep, b/c reload depends on these bnds
      ^{:out :inherit}
      (proc/$ clawe reload)
      proc/check :out slurp)))

(defkbd clawe-restart
  [[:mod] "r"]
  {:binding/awm true}
  ;; TODO support just passing in a defcom
  ;; TODO consider moving keybindings into domain files?
  (defcom/exec restart))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom reload
  "Write all dependent configs and restart whatever daemons."
  (do
    (log "reloading...")

    ;; Bindings
    (when-not notify/is-mac?
      (log "rewriting awm bindings")
      (awm.bindings/write-awesome-bindings)
      (log "resetting sxhkd bindings")
      ;; NOTE this ensures the sxhkd tmux session as well, which is required for keybindings to work!
      (sxhkd.bindings/reset-bindings))

    ;; Rules
    (when notify/is-mac?
      (workspaces/do-yabai-correct-workspaces))

    (when-not notify/is-mac?
      (log "rewriting rules")
      (awm.rules/write-awesome-rules)
      (log "reapplying rules")
      (c.rules/apply-rules)
      (c.rules/correct-clients-and-workspaces)
      (log "finished rules"))


    ;; Restart Notifications service
    (when-not notify/is-mac?
      (log "reloading notifications")
      (-> (proc/$ systemctl --user start deadd-notification-center)
          (proc/check)))

    ;; Reload completions/caches
    (when-not notify/is-mac?
      (log "reloading zsh tab completion")
      (c.install/install-zsh-tab-completion))

    ;; Doom env refresh - probably a race-case here....
    (r.tmux/fire {:tmux.fire/cmd     "doom env"
                  :tmux.fire/session "dotfiles"})
    (r.emacs/fire "(doom/reload-env)")

    ;; Doctor - Wallpaper, etc
    (when-not notify/is-mac?
      (log "Firing doctor reload")
      (clawe.doctor/reload))

    (log "Reload complete")))

(comment
  (defcom/exec reload))
