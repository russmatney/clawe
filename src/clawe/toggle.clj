(ns clawe.toggle
  (:require
   [ralphie.awesome :as awm]
   [ralphie.browser :as r.browser]
   [ralphie.emacs :as r.emacs]
   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.yabai :as yabai]
   [ralphie.zsh :as r.zsh]

   [clawe.client :as client]
   [clawe.config :as config]
   [clawe.workspaces :as workspaces]
   [clojure.string :as string]))

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
  ([{:keys [should-float-and-center]}
    {:keys [wsp->client wsp->open-client
            clients->client client->hide client->show
            ->ensure-workspace]}]
   (let [yb-windows     (yabai/query-windows)
         current-wsp    (workspaces/current-workspace-fast
                          {:prefetched-windows yb-windows})
         client         (wsp->client current-wsp)
         client-focused (or
                          (:yabai.window/has-focus client)
                          (:awesome.client/focused client))]
     (cond
       ;; no tag, maybe create one? maybe prompt for a name?
       (not current-wsp)
       (do
         (awm/create-tag! "temp-tag")
         (awm/focus-tag! "temp-tag")
         (wsp->open-client nil))

       ;; client is focused, let's close/hide/send-it-away
       (and client client-focused)
       (cond
         client->hide
         (do
           (when ->ensure-workspace
             ;; support creating the target workspace first
             ;; (shouldn't this just be part of client->hide ?)
             (->ensure-workspace))
           (client->hide client))
         ;; TODO is-mac? should be in clawe.config/is-mac?
         ;; TODO move to client/workspace protocol or multi-method
         (clawe.config/is-mac?) (yabai/close-window client)
         :else                  (awm/close-client client))

       ;; client found in workspace, not focused
       (and client (not client-focused))
       (cond
         (clawe.config/is-mac?)
         (do
           (yabai/focus-window client)
           ;; TODO not for terminal/emacs?
           ;; float and center opt-out option
           (when should-float-and-center
             (yabai/float-and-center-window client)))

         :else
         (client/focus-client
           {:center?   should-float-and-center
            :float?    should-float-and-center
            :bury-all? false} client))

       ;; we have a current workspace, but no client in it
       ;; (not client)
       :else
       (let [client (when clients->client (clients->client yb-windows))]
         (cond
           (and client client->show)
           (client->show client current-wsp)

           :else
           (wsp->open-client current-wsp))))
     ;; prevent noise in the logs
     nil)))

(comment
  (some->>
    (workspaces/current-workspace)
    workspaces/merge-awm-tags))

(defn toggle-scratchpad-app
  "Provides some overwrites to the `toggle-client` impl
  that can create/ensure workspaces for the toggling clients,
  so there's a place to send them.
  "
  [{:keys [space-label is-client?]}]
  {:wsp->client
   (fn [{:awesome.tag/keys [clients] :yabai/keys [windows] :as wsp}]
     (some->> (concat clients windows) (filter #(is-client? wsp %)) first))

   :->ensure-workspace
   (fn [] (yabai/create-and-label-space
            {:space-label       space-label
             :overwrite-labeled true}))

   :clients->client
   (fn [clients] (some->> clients (filter #(is-client? nil %)) first))

   :client->hide
   (fn [c]
     ;; TODO what to do when we're already on this space-label?
     ;; TODO toy with switching to an osx-hide instead, might create less space-noise,
     ;; and maybe be preferrable... not sure how the interactions with cmd-tab will be
     (yabai/move-window-to-space c space-label))

   :client->show
   (fn [c wsp]
     (yabai/float-and-center-window c)
     (yabai/move-window-to-space c (or (:yabai.space/index wsp) (:workspace/title wsp)))
     ;; focus last so osx doesn't move spaces on us
     (yabai/focus-window c))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle-app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-matches-window-title? [{:keys [window-title]} {:as wsp}
                                    client]
  ;; maybe a partial match is better?
  ;; or accept/coerce regex from the command line?
  ;; TODO pull from proper :client/window-title
  (string/includes?
    (:yabai.window/title client (:awesome.client/name client))

    ;; could skip for now, it's an edge case to have multiple emacs/alacritty
    ;; open on the same space on osx
    (if (#{"match-workspace"} window-title)
      (:workspace/title wsp)
      window-title)))

(defn client-matches-app-name? [{:keys [app-name]} _wsp client]
  ;; TODO pull from proper :client/app-name
  (#{app-name} (:yabai.window/app client (:awesome.client/class client))))

(defn is-client?
  [{:keys [window-title app-name] :as args} wsp client]
  (cond
    (and window-title app-name)
    (and (client-matches-window-title? args wsp client)
         (client-matches-app-name? args wsp client))
    window-title (client-matches-window-title? args wsp client)
    app-name     (client-matches-app-name? args wsp client)))

(def name->open-client
  {"journal"
   (fn [_wsp]
     (let [opts {:emacs.open/workspace "journal"
                 :emacs.open/file      "/Users/russ/todo/journal.org"}]
       (r.emacs/open opts)))
   "web"
   (fn [_wsp] (r.browser/open))
   "devweb"
   (fn [_wsp] (r.browser/open-dev))
   "emacs"
   (fn [_wsp] ;; does this passed 'fast-wsp' have enough already?
     ;; TODO support initial-file/dir, initial-command as input
     (let [{:workspace/keys [title initial-file directory]
            :git/keys       [repo]
            :as             wsp} (workspaces/current-workspace)]
       (if-not wsp
         (r.emacs/open)
         (let [initial-file (or initial-file repo directory)
               opts         {:emacs.open/workspace title :emacs.open/file initial-file}]
           (r.emacs/open opts)))))
   "terminal"
   (fn [_wsp] ;; does this passed 'fast-wsp' have enough already?
     ;; TODO support directory, initial-command as input
     ;; TODO support tmux.fire as input
     (let [{:workspace/keys [title directory]
            :git/keys       [repo]
            :as             wsp} (workspaces/current-workspace)]
       (if-not wsp
         (r.tmux/open-session)
         (let [directory (or directory repo (r.zsh/expand "~"))
               opts      {:tmux/session-name title :tmux/directory directory}]
           (r.tmux/open-session opts)))))})

(defn toggle-app
  {:org.babashka/cli
   {:alias {:title  :window-title
            :app    :app-name
            :wsp    :workspace-name
            :client :client-name ;; tryna get emacs/term to work with wsp-name
            }}}
  [{:keys [workspace-name client-name] :as args}]
  (let [wsp->open-client (name->open-client (or workspace-name client-name))]
    (toggle-client
      {;; emacs and terminal toggle opt-out of float-and-center
       :should-float-and-center
       (not (#{"emacs" "terminal"} client-name))}
      (merge
        (if workspace-name
          (toggle-scratchpad-app
            {:space-label workspace-name
             :is-client?  (partial is-client? args)})
          ;; rn if we don't have a workspace-name, we only want
          ;; this piece of toggle-scratchpad-app
          ;; TODO clean this up!
          {:wsp->client
           (:wsp->client
            (toggle-scratchpad-app
              {:is-client? (partial is-client? args)}))})
        {:wsp->open-client
         (or wsp->open-client
             (fn [{:as wsp}]
               (notify/notify (str "To do: open " (or workspace-name
                                                      client-name)))
               (println wsp)))}))))
