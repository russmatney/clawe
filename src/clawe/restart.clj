(ns clawe.restart
  (:require
   [cheshire.core :as json]

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
   [ralphie.emacs :as r.emacs]))

;; dumping here as part of restart process

(defn write-commands-to-json-fn []
  (let [f (zsh/expand "~/russmatney/clawe/commands.json")]
    (->>
      (defcom/list-commands)
      (map (fn [{:keys [name ns doc]}]
             {;; super-secret-special-alfred-keys
              ;; https://www.alfredapp.com/help/workflows/inputs/script-filter/json/
              :arg          name
              :title        name
              :autocomplete name
              :subtitle     (or doc "")

              :mods
              {:cmd {:arg      (str "open-in-emacs " name)
                     :subtitle "Open In Emacs"}}

              :name name
              :ns   ns
              :doc  doc}))
      (#(json/generate-string % {:pretty true}))
      (spit f))))

(defcom write-commands-to-json
  (write-commands-to-json-fn))


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

(comment
  (do
    (log "unit test run")
    (check-unit-tests)
    (log "things continued!")))

(defcom run-clawe-unit-tests
  check-unit-tests)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom restart
  "Rebuild clawe, plus Reload.

At system startup, `clawe reload` is important, as it ensures that a number
of things are in place, most critially the sxhkd service and tmux session.

Rebuild requires the unit tests to pass before rebuilding the clawe executable, which is an uberjar.

The uberjar can be manually rebuilt on the command line with:

bb -cp $(clojure -Spath) --uberjar clawe.jar -m clawe.core # rebuild clawe

See `clawe.install/build-uberjar`.
"
  (do
    (log "Restarting...")
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
        (log "Strange unit tests response...")))

    (log "reloading clawe...")
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

    ;; TODO try/catch these reload sections

    ;; Bindings
    (when-not notify/is-mac?
      (log "rewriting awm bindings")
      (awm.bindings/write-awesome-bindings)
      (log "resetting sxhkd bindings")
      ;; NOTE this ensures the sxhkd tmux session as well, which is required for keybindings to work!
      (sxhkd.bindings/reset-bindings))

    ;; Rules
    (if notify/is-mac?
      (workspaces/do-yabai-correct-workspaces)
      (do
        (log "rewriting rules")
        (awm.rules/write-awesome-rules)
        (log "reapplying rules")
        (c.rules/apply-rules)
        (c.rules/correct-clients-and-workspaces)
        (log "finished rules")))


    ;; Notifications
    (when-not notify/is-mac?
      (log "reloading notifications")
      ;; TODO untested - i'm hoping this saves the manual effort at startup
      (-> (proc/$ systemctl --user start deadd-notification-center)
          (proc/check)))

    ;; Reload completions/caches
    (when-not notify/is-mac?
      (log "reloading zsh tab completion")
      (c.install/install-zsh-tab-completion)
      (log "writing commands to commands.json")
      (write-commands-to-json-fn))

    ;; Doom env refresh - probably a race-case here....
    (r.tmux/fire {:tmux.fire/cmd     "doom env"
                  :tmux.fire/session "dotfiles"})
    (r.emacs/fire "(doom/reload-env)")

    ;; Doctor - Wallpaper
    (when-not notify/is-mac?
      (log "reloading doctor")
      (slurp "http://localhost:3334/reload"))

    ;; considering...
    ;; (-> (proc/$ systemctl --user restart doctor-topbar)
    ;;     (proc/check))

    (log "completed.")))

(comment
  (defcom/exec reload))
