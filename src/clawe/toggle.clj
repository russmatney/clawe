(ns clawe.toggle
  (:require
   [clojure.string :as string]

   [ralphie.browser :as r.browser]
   [ralphie.emacs :as r.emacs]
   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]

   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.client :as client]
   [clawe.workspace :as workspace]
   [clawe.config :as clawe.config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; find-client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this might be the same as wm/fetch-client... presuming that is written
;; to support passed :clients or :prefetched-clients
(defn find-client
  "Returns a client matching the passed `client-def`.

  Matches via `client/match?`, passing the def twice to use any `:match/` opts.

  Supports prefetched `:prefetched-clients` to avoid the `wm/` call."
  ([client-def] (find-client nil client-def))
  ([opts client-def]
   (let [all-clients (:prefetched-clients opts (wm/active-clients opts))]
     (some->> all-clients
              (filter
                ;; pass def as opts
                (partial client/match? client-def client-def))
              first))))

(defn ensure-client
  ([client-def] (ensure-client nil client-def))
  ([opts client-def]
   (when-not (find-client opts client-def)
     (println "todo: create client" client-def))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; in current workspace?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-in-current-workspace?
  ([client] (client-in-current-workspace? nil client))
  ([opts client]
   (when client
     (let [wsps (:current-workspaces opts (wm/current-workspaces
                                            ;; pass :prefetched-clients through
                                            (assoc opts :include-clients true)))]
       (some->> wsps
                (map (fn [wsp] (workspace/find-matching-client wsp client)))
                first)))))

(comment
  (clawe.config/reload-config)
  (def clawe-emacs-client
    (->
      (clawe.config/client-def "web")
      find-client))

  (->>
    (wm/active-clients)
    (map client/strip)
    )


  (client/match?
    (clawe.config/client-def "journal")
    (clawe.config/client-def "journal")
    clawe-emacs-client)

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle client def
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn determine-toggle-action [client-key]
  (println "deter w/ client-key" client-key)
  ;; TODO prefetch and pass opts into funcs
  (let [def (clawe.config/client-def client-key)]
    (if-not def
      [:no-def client-key]

      (let [client (find-client def)]
        (cond
          (not client) [:create-client def]

          (client-in-current-workspace? client)
          (if (:client/focused client)
            [:hide-client client]
            [:focus-client client])

          :else [:show-client client])))))

;; TODO rofi for choosing one of these events with any client in the config

(defn toggle
  {:org.babashka/cli
   {:alias {:key :client/key
            ;; TODO might include an override for :hide/type, :open/* options
            }}}
  [args]
  (notify/notify "[toggle]" (:client/key args "No --key"))
  (let [[action
         ;; TODO better var name than val
         ;; client-or-def ?
         ;; this is why reframe did maps for everything
         ;; gotta do keys when it's ambigous
         val] (determine-toggle-action (:client/key args))]
    (println "action:" action)
    (case action
      :no-def
      (do
        (println "WARN: [toggle] no def found for client-key" args)
        (notify/notify "[no def]" (:client/key args "No --key")))

      ;; emacs/tmux/generic opts/description
      :create-client
      (do
        (println "open" (client/strip val))
        (notify/notify "[:create-client]" (:client/key args "No --key")))

      :hide-client
      (do
        ;; move to an ensured workspace? close? minimize? hide?
        (println "hide" (client/strip val))
        (notify/notify "[:hide-client]")
        (wm/hide-client val))

      :focus-client
      (do
        (println "focus" (client/strip val))
        (notify/notify "[:focus-client]")
        ;; this is sometimes 'send-focus' other times 'jumbotron'
        (wm/focus-client {:float-and-center
                          ;; won't apply to emacs/term... fine for now
                          true} val))

      :show-client
      (do
        (println "show" (client/strip val))
        (notify/notify "[:show-client]")
        ;; TODO consider 'wm/show-client' or :show/ opts
        ;; or a more :hyde/jekyll or toggle->focus function
        (wm/move-client-to-workspace
          ;; TODO how to cache this extra lookup... for free?
          ;; wm could hold a cache? or just support a 'current'
          ;; opt for this function - the wm might have a shortcut
          ;; .... or just call it in a let and pass it all the way through
          val (wm/current-workspace))
        (wm/focus-client {:float-and-center
                          ;; won't apply to emacs/term... fine for now
                          true} val) )

      (println "No matching action for:" action))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App toggling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle-client
  "Helper for toggling a client in the current workspace.

  `:wsp->client` is a function that returns the client to-be-toggled from a list of
  the current clients in the workspace.

  `:wsp->open-client` is a function to create the client in the context of the
  workspace. Note that this may be called in the context of no workspace at all.

  # TODO refactor into a description -> exec description model
  should be well-tested, with examples of weird client-titles like
  `clawe - (159 x 49)` <-- osx emacs adding dimensions?
  "
  ([desc] (toggle-client nil desc))
  ([{:keys [should-float-and-center workspace-title]}
    {:keys [wsp->client wsp->open-client
            all-clients->client client->hide client->show]}]
   (let [clients     (wm/active-clients)
         current-wsp (wm/current-workspace {:prefetched-clients clients})
         client      (wsp->client current-wsp)]
     (cond
       ;; no tag, let's create one
       (not current-wsp)
       (do
         (wm/focus-workspace (or ;; NOTE this is the intended workspace title
                               workspace-title "home"))
         (wsp->open-client nil))

       ;; client is focused, let's close/hide/send-it-away
       (and client (:client/focused client))
       (cond
         client->hide (client->hide client)
         :else        (wm/close-client client))

       ;; client found in workspace, not focused
       (and client (not (:client/focused client)))
       (wm/focus-client {:float-and-center should-float-and-center} client)

       ;; we have a current workspace, but no client in it
       ;; (not client)
       :else
       (let [client (when all-clients->client (all-clients->client clients))]
         (cond
           (and client client->show)
           (client->show client current-wsp)

           :else
           (wsp->open-client current-wsp))))
     (clawe.doctor/update-topbar)
     ;; prevent noise in the logs
     nil)))

(defn toggle-scratchpad-app
  "Provides some overwrites to the `toggle-client` impl
  that can create/ensure workspaces for the toggling clients,
  so there's a place to send them.
  "
  [{:keys [workspace-title is-client?]}]
  {:wsp->client
   (fn [{:workspace/keys [clients] :as wsp}]
     (some->> clients (filter #(is-client? wsp %)) first))

   :all-clients->client
   (fn [clients] (some->> clients (filter #(is-client? nil %)) first))

   :client->hide
   (fn [c]
     ;; TODO what to do when we're already on this workspace ?
     ;; TODO toy with switching to an osx-hide instead, might create less space-noise,
     ;; and maybe be preferrable... not sure how the interactions with cmd-tab will be
     (wm/move-client-to-workspace {:ensure-workspace true} c workspace-title))

   :client->show
   (fn [c wsp]
     (wm/move-client-to-workspace c wsp)
     (wm/focus-client {:float-and-center true} c))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle-app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-matches-window-title?
  "Returns true if the args' window-title matches the client window-title.

  If the args' window-title is 'match-workspace', we compare the
  client/window-title to the workspace/title.
  "
  [{:keys [window-title]} {:as wsp} client]
  ;; maybe a partial match is better?
  ;; or accept/coerce regex from the command line?
  (string/includes?
    (:client/window-title client)

    ;; could skip for now, it's an edge case to have multiple emacs/alacritty
    ;; open on the same space on osx
    ;; TODO this is ugly, use a flag
    (if (#{"match-workspace"} window-title)
      (:workspace/title wsp)
      window-title)))

(defn client-matches-app-name? [{:keys [app-name]} _wsp client]
  (#{app-name} (:client/app-name client)))

(defn is-client?
  "Returns true if the passed `client` matches the passed `window-title` and/or `app-name`.

  Supports a few use-cases, including:
  - Singleton clients that can match on one app-name
    - e.g. Spotify, Discord, Firefox
  - Per-workspace emacs/terminal clients
    - e.g. app-name matches Emacs, window-title matches the workspace-title
      (this terminal or emacs client belongs to this workspace)
  - Scratchpads that require an app-name AND window-title match
    - e.g. a 'journal' titled emacs scratchpad"
  [{:keys [window-title app-name] :as args} wsp client]
  (cond
    (and window-title app-name)
    (and (client-matches-window-title? args wsp client)
         (client-matches-app-name? args wsp client))
    window-title (client-matches-window-title? args wsp client)
    app-name     (client-matches-app-name? args wsp client)))

;; TODO move into :exec data in something like config/client-defs
;; should be able to configure emacs, tmux, misc fully-qualified func calls
;; via bb-cli
(def client-config-id->open-client
  {"journal"
   (fn [_wsp]
     (let [opts {:emacs.open/workspace "journal"
                 :emacs.open/file      "~/todo/journal.org"}]
       (r.emacs/open opts)))
   "web"
   (fn [_wsp] (r.browser/open))
   "devweb"
   (fn [_wsp] (r.browser/open-dev))
   "emacs"
   (fn [_wsp] ;; does this passed 'fast-wsp' have enough already?
     ;; TODO support initial-file/dir, initial-command as input
     (let [{:workspace/keys [title initial-file directory] :as wsp}
           (wm/current-workspace)]
       (if-not wsp
         (r.emacs/open)
         (let [initial-file (or initial-file directory)
               opts         {:emacs.open/workspace title :emacs.open/file initial-file}]
           (r.emacs/open opts)))))
   "terminal"
   (fn [_wsp] ;; does this passed 'fast-wsp' have enough already?
     ;; TODO support directory, initial-command as input
     ;; TODO support tmux.fire as input
     (let [{:workspace/keys [title directory] :as wsp}
           (wm/current-workspace)]
       (if-not wsp
         (r.tmux/open-session)
         (let [directory (or directory (r.zsh/expand "~"))
               opts      {:tmux/session-name title :tmux/directory directory}]
           (r.tmux/open-session opts)))))})

(defn toggle-app
  {:org.babashka/cli
   {:coerce {:use-current :boolean}
    :alias  {:wsp          :workspace/title ;; support :current or "current" for wsp here
             :window-title :client/window-title
             :client-id    :client/config-id
             :app          :client/app-name}}}
  [{:keys [workspace/title client/config-id] :as args}]
  (println "toggle-app args" args)
  (let [
        ;; TODO pull from client-config
        use-current-wsp? (#{"current" :current} title)
        wsp->open-client
        ;; TODO pull from client-config
        (client-config-id->open-client config-id)]
    (toggle-client
      {;; emacs and terminal toggle opt-out of float-and-center
       :should-float-and-center
       false
       ;; TODO pull from client-config
       }
      (merge
        (if title
          (toggle-scratchpad-app
            {:workspace-title title
             :is-client?      (partial is-client? args)})
          ;; rn if we don't have a workspace/title, we only want
          ;; this piece of toggle-scratchpad-app
          ;; TODO clean this up!
          {:wsp->client
           (:wsp->client
            (toggle-scratchpad-app
              {:is-client? (partial is-client? args)}))})
        {:wsp->open-client
         (or wsp->open-client
             (fn [{:as wsp}]
               (notify/notify (str "To do: open " config-id))
               (println wsp)))}))))
