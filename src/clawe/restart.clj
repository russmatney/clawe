(ns clawe.restart
  (:require
   [babashka.process :as proc]
   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]

   [clawe.install :as c.install]
   [clawe.rules :as c.rules]
   [clawe.awesome.rules :as awm.rules]
   [clawe.awesome.bindings :as awm.bindings]
   [clawe.sxhkd.bindings :as sxhkd.bindings]
   [ralphie.zsh :as r.zsh]
   [ralphie.sh :as r.sh]
   [ralphie.tmux :as r.tmux]
   [ralphie.emacs :as r.emacs]))

(defn log [msg]
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.restart
       :notify/subject msg
       :notify/body    :clawe.restart})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom restart
  "Rebuilds the clawe executable, which is an uberjar.

Fires `clawe reload`, which is implemented below.

We want to make sure the new config is written with the updated
uberjar. Otherwise this might need to be called twice."
  (do
    (log "restarting...")
    ;; maybe detect if the current uberjar is out of date?
    ;; might not always need to rebuild here
    (c.install/build-uberjar)
    (log "rebuilt uberjar...")
    ;; maybe a pause or file read/watch, something that shows it's new?
    ;; or at least some log that reveals whether it is new or not
    ;; log and compare the checksum of the uberjar, with a fallback

    ;; if nothing has changed, maybe just call rewrite-and-reload from here
    (log "reloading...")
    (->
      ;; kicking to process here could mean we use the new uberjar
      ;; (if the prev command waits for completion, which it seems to)
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
    (log "rewriting awm bindings")
    (awm.bindings/write-awesome-bindings)
    (log "resetting sxhkd bindings")
    (sxhkd.bindings/reset-bindings)

    ;; Rules
    (log "rewriting rules")
    (awm.rules/write-awesome-rules)
    (log "reapplying rules")
    (c.rules/apply-rules)
    (c.rules/correct-clients-and-workspaces)

    ;; Notifications
    (log "reloading notifications")
    ;; TODO untested - i'm hoping this saves the manual effort at startup
    (-> (proc/$ systemctl --user start deadd-notification-center)
        (proc/check))

    ;; Misc
    (log "reloading misc")
    (awm/reload-misc)

    ;; Reload completions/caches
    (log "reloading zsh tab completion")
    (c.install/install-zsh-tab-completion)

    ;; Widgets
    ;; DEPRECATED
    ;; (log "reloading widgets")
    ;; (awm/reload-bar-and-widgets)

    ;; Doom env refresh - probably a race-case here....
    (r.tmux/fire "doom env")
    (r.emacs/fire "(doom/reload-env)")

    ;; Doctor - Wallpaper
    (log "reloading doctor")
    (slurp "http://localhost:3334/reload")

    ;; considering...
    ;; (-> (proc/$ systemctl --user restart doctor-topbar)
    ;;     (proc/check))

    (log "completed.")))

(comment
  (defcom/exec reload))
