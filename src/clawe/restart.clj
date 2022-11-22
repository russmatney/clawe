(ns clawe.restart
  (:require
   [babashka.process :as proc]

   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]
   [ralphie.emacs :as emacs]
   [ralphie.install :as r.install]
   [ralphie.notify :as notify]
   [ralphie.tmux :as tmux]
   [babashka.fs :as fs]

   ;; required to put bindings in place, otherwise we write empty rc configs
   clawe.defs.bindings
   [clawe.awesome.bindings :as awm.bindings]
   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]
   [clawe.rules :as rules]
   [clawe.sxhkd.bindings :as sxhkd.bindings]))

(defn log [msg]
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.restart
       :notify/subject msg
       :notify/body    :clawe.restart})))

(defn install-zsh-tab-completion []
  (r.install/install-zsh-completion "clawe"))

(defcom install-clawe-zsh-tab-completion
  (install-zsh-tab-completion))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; check if unit tests are passing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-unit-tests
  "Run the unit tests, return [:success ...] or [:fail ....]."
  []
  (let [proc
        ^{:out :string
          ;; TODO use clawe-dir (config?)
          :dir (str (fs/home) "/russmatney/clawe")}
        (proc/$ bb test-unit)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare reload)

(defkbd clawe-reload
  [[:mod] "r"]
  (reload))

(defn reload
  "Write all dependent configs and restart whatever daemons."
  ([] (reload nil))
  ([_]
   (log "reloading...")
   ;; Bindings
   (when-not (clawe.config/is-mac?)
     (log "rewriting awm bindings")
     (awm.bindings/write-awesome-bindings)
     (log "resetting sxhkd bindings")
     ;; NOTE this ensures the sxhkd tmux session as well, which is required for keybindings to work!
     (sxhkd.bindings/reset-bindings))

   (rules/clean-up-workspaces)

   ;; Restart Notifications service
   (when-not (clawe.config/is-mac?)
     (log "reloading notifications")
     (-> (proc/$ systemctl --user start deadd-notification-center)
         (proc/check)))

   ;; Reload completions/caches
   (when-not (clawe.config/is-mac?)
     (log "reloading zsh tab completion")
     (install-zsh-tab-completion))

   ;; Doom env refresh - probably a race-case here....
   (tmux/fire {:tmux.fire/cmd     "doom env"
               :tmux.fire/session "dotfiles"})
   (emacs/fire "(doom/reload-env)")

   ;; Doctor - Wallpaper, etc
   (when-not (clawe.config/is-mac?)
     (log "Firing doctor reload")
     (clawe.doctor/reload))

   (clawe.doctor/update-topbar)
   (log "Reload complete")

   (check-unit-tests)))
