(ns clawe.restart
  (:require
   [babashka.process :refer [$ check] :as proc]

   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]
   [ralphie.emacs :as emacs]
   [ralphie.install :as r.install]
   [ralphie.notify :as notify]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]
   [util :as util]

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
;; Build Clawe Uberjar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-uberjar
  "Rebuilds the clawe uberjar. Equivalent to:

  bb -cp $(clojure -Spath) --uberjar clawe.jar -m clawe.core # rebuild clawe

  on the command line."
  []
  (let [notif (fn [s] (notify/notify
                        {:subject s :replaces-process "rebuilding-clawe-uberjar"}))
        dir   (zsh/expand "~/russmatney/clawe")]
    (notif "Clawe Uberjar: Rebuilding")
    (let [cp (util/get-cp dir)]
      (->
        ^{:dir dir}
        ($ bb -cp ~cp --uberjar clawe.jar -m clawe.core )
        check)
      (notif "Clawe Uberjar: Rebuild Complete"))))

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
          (build-uberjar)
          (catch Exception e
            (notify/notify "Exception while rebuilding uberjar")
            (println e))))

      :else
      (log "Strange unit tests response..."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do-reload
  "Write all dependent configs and restart whatever daemons."
  ([] (do-reload nil))
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
   (log "Reload complete")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn restart
  "Maybe rebuild the clawe uberjar, then call `reload`.

  Rebuild requires the unit tests to pass, but if they fail, `reload` is called anyway.

  The uberjar can be manually rebuilt on the command line with:

  bb -cp $(clojure -Spath) --uberjar clawe.jar -m clawe.core # rebuild clawe
  See `build-uberjar`.
  "
  []
  (log "Restarting...")
  (attempt-uberjar-rebuild)

  ;; reload afterwards, pretty much no matter what
  (log "reloading clawe...")
  (do-reload))

(defkbd clawe-restart
  [[:mod] "r"]
  {:binding/awm true}
  ;; TODO support just passing in a defcom
  ;; TODO consider moving keybindings into domain files?
  (restart))
