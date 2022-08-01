(ns clawe.toggle
  (:require
   [defthing.defkbd :refer [defkbd]]

   [ralphie.awesome :as awm]
   [ralphie.browser :as r.browser]
   [ralphie.emacs :as r.emacs]
   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.yabai :as yabai]
   [ralphie.zsh :as r.zsh]

   [clawe.client :as client]
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
  "
  [{:keys [wsp->client wsp->open-client
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

      ;; client is focused, let's close/hide/send-it-away it
      (and client client-focused)
      (cond
        client->hide
        (do
          (when ->ensure-workspace
            ;; support creating the target workspace
            (->ensure-workspace))
          (client->hide client))
        notify/is-mac? (yabai/close-window client)
        :else          (awm/close-client client))

      ;; client found in workspace, not focused
      (and client (not client-focused))
      (cond
        notify/is-mac?
        (do
          (yabai/focus-window client)
          ;; TODO not for terminal/emacs?
          (yabai/float-and-center-window client))

        :else
        ;; DEPRECATED
        (client/focus-client {:center?   false
                              :float?    false
                              :bury-all? false} client))

      ;; we have a current workspace, but no client in it
      ;; (not client)
      :else
      (let [client (when clients->client (clients->client yb-windows))]
        (cond
          (and client client->show)
          (client->show client current-wsp)

          :else
          (wsp->open-client current-wsp)
          )))
    ;; prevent noise in the logs
    nil))

(comment
  (some->>
    (workspaces/current-workspace)
    workspaces/merge-awm-tags)
  )

(defn toggle-scratchpad-app [{:keys [space-label is-client?]}]
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
     (yabai/move-window-to-space c space-label))

   :client->show
   (fn [c wsp]
     (yabai/float-and-center-window c)
     (yabai/move-window-to-space c (or (:yabai.space/index wsp) (:workspace/title wsp)))
     ;; focus last so osx doesn't move spaces on us
     (yabai/focus-window c))})


(defn is-terminal? [wsp w]
  (let [title (:workspace/title wsp)]
    (or
      (and
        (-> w :awesome.client/class #{"Alacritty"})
        (-> w :awesome.client/name #{title}))
      (-> w :yabai.window/app #{"Alacritty"}))))

(defkbd toggle-terminal
  [[:mod] "Return"]
  (toggle-client
    {:wsp->client
     (fn [{:awesome.tag/keys [clients] :yabai/keys [windows] :as wsp}]
       (some->> (concat clients windows) (filter #(is-terminal? wsp %)) first))
     :wsp->open-client
     ;; TODO could fetch the full current-workspace here
     (fn [_fast-wsp]
       (let [{:workspace/keys [title directory]
              :git/keys       [repo]
              :as             wsp}
             (workspaces/current-workspace)]
         (if-not wsp
           (r.tmux/open-session)
           (let [directory (or directory repo (r.zsh/expand "~"))
                 opts      {:tmux/session-name title :tmux/directory directory}]
             (r.tmux/open-session opts)))))}))

(defn is-emacs? [wsp w]
  (let [title (:workspace/title wsp)]
    (or
      (and
        (-> w :awesome.client/class #{"Emacs"})
        (-> w :awesome.client/name #{title}))
      (-> w :yabai.window/app #{"Emacs"}))))

(defkbd toggle-emacs
  [[:mod :shift] "Return"]
  (toggle-client
    {:wsp->client
     (fn [{:awesome.tag/keys [clients] :yabai/keys [windows] :as wsp}]
       (some->> (concat clients windows) (filter #(is-emacs? wsp %)) first))
     :wsp->open-client
     (fn [_fast-wsp]
       (let [{:workspace/keys [title initial-file directory]
              :git/keys       [repo]
              :as             wsp}
             (workspaces/current-workspace)]
         (if-not wsp
           (r.emacs/open)
           (let [initial-file (or initial-file repo directory)
                 opts         {:emacs.open/workspace title :emacs.open/file initial-file}]
             (r.emacs/open opts)))))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle-app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-matches-window-title? [{:keys [window-title]} {:as wsp}
                                    client]
  ;; maybe a partial match is better?
  ;; or accept/coerce regex from the command line?
  ;; TODO move to proper :client/window-title
  (string/includes?
    (:yabai.window/title client (:awesome.client/name client))

    ;; could skip for now, it's an edge case to have multiple emacs/alacritty
    ;; open on the same space on osx
    (if (#{"match-workspace"} window-title)
      (:workspace/title wsp)
      window-title)))

(defn client-matches-app-name? [{:keys [app-name]} _wsp client]
  ;; TODO move to proper :client/app-name
  (#{app-name} (:yabai.window/app client (:awesome.client/class client))))

(defn is-client?
  [{:keys [window-title app-name] :as args} wsp client]
  (cond
    (and window-title app-name)
    (and (client-matches-window-title? args wsp client)
         (client-matches-app-name? args wsp client))
    window-title (client-matches-window-title? args wsp client)
    app-name     (client-matches-app-name? args wsp client)))

(def wsp-name->open-client
  {"journal"
   (fn [_wsp]
     (let [opts {:emacs.open/workspace "journal"
                 :emacs.open/file      "/Users/russ/todo/journal.org"}]
       (r.emacs/open opts)))
   "web"
   (fn [_wsp] (r.browser/open))
   "devweb"
   (fn [_wsp] (r.browser/open-dev))})

(defn toggle-app
  {:org.babashka/cli
   {:alias {:title :window-title
            :app   :app-name
            :wsp   :workspace-name}}}
  [{:keys [workspace-name] :as args}]
  (toggle-client
    (merge
      (toggle-scratchpad-app
        {:space-label workspace-name
         :is-client?  (partial is-client? args)})
      {:wsp->open-client
       (fn [{:as wsp}]
         (if-let [open-client (wsp-name->open-client workspace-name)]
           (open-client wsp)
           (do
             (notify/notify (str "To do: open " workspace-name))
             (println wsp))))})))
