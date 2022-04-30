(ns clawe.toggle
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]

   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   [ralphie.emacs :as r.emacs]
   [ralphie.browser :as r.browser]

   [clawe.awesome :as c.awm] ;; DEPRECATED
   [clawe.workspaces :as workspaces]

   [ralphie.awesome :as awm]
   [ralphie.yabai :as yabai]))


(defn is-journal? [_wsp c]
  (or
    (and
      (-> c :awesome.client/class #{"Emacs"})
      (-> c :awesome.client/name #{"journal"}))
    (and
      (-> c :yabai.window/app #{"Emacs"})
      (-> c :yabai.window/title (#(re-seq #"^journal" %))))))

(defn is-web? [_wsp c]
  (or
    (-> c :awesome.client/class #{"Firefox"})
    (-> c :yabai.window/app #{"Safari"})))

(defn is-dev-web? [_wsp c]
  (or
    (-> c :yabai.window/app #{"Firefox Developer Edition"})))

(defn is-slack? [_wsp c]
  (or
    (-> c :awesome.client/class #{"Slack"})
    (-> c :yabai.window/app #{"Slack"})))

(defn is-messages? [_wsp c]
  (-> c :yabai.window/app #{"Messages"}))

(defn is-godot? [_wsp c]
  (or
    (-> c :awesome.client/class #{"Godot"})
    (-> c :yabai.window/app #{"Godot"})))

(defn is-spotify? [_wsp c]
  (or
    (-> c :awesome.client/class #{"Spotify"})
    (-> c :yabai.window/app #{"Spotify"})))

(defn is-terminal? [wsp w]
  (let [title (:workspace/title wsp)]
    (or
      (and
        (-> w :awesome.client/class #{"Alacritty"})
        (-> w :awesome.client/name #{title}))
      (-> w :yabai.window/app #{"Alacritty"}))))

(defn is-emacs? [wsp w]
  (let [title (:workspace/title wsp)]
    (or
      (and
        (-> w :awesome.client/class #{"Emacs"})
        (-> w :awesome.client/name #{title}))
      (-> w :yabai.window/app #{"Emacs"}))))

(comment
  (re-seq #"^journal" "journal - (126 x 65)")
  (re-seq #"^journal" "something else"))

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
        (c.awm/focus-client {:center?   false
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

(defcom toggle-journal-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "journal" :is-client? is-journal?})
      {:wsp->open-client
       (fn [{:as _wsp}]
         (let [opts { ;; TODO get from defworkspace journal
                     :emacs.open/workspace "journal"
                     :emacs.open/file      "/Users/russ/todo/journal.org"}]
           (r.emacs/open opts)))})))

(defcom toggle-web-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "web" :is-client? is-web?})
      {:wsp->open-client (fn [_wsp] (r.browser/open))})))

(defcom toggle-dev-web-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "dev-web" :is-client? is-dev-web?})
      {:wsp->open-client (fn [_wsp] (r.browser/open-dev))})))

(defcom toggle-slack-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "slack" :is-client? is-slack?})
      {:wsp->open-client
       (fn [{:as wsp}]
         (notify/notify "To do: open slack")
         (println wsp))})))

(defcom toggle-spotify-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "spotify" :is-client? is-spotify?})
      {:wsp->open-client
       (fn [{:as wsp}]
         (notify/notify "To do: open spotify")
         (println wsp))})))

(defcom toggle-messages-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "messages" :is-client? is-messages?})
      {:wsp->open-client
       (fn [{:as wsp}]
         (notify/notify "To do: open messages")
         (println wsp))})))

(defcom toggle-godot-2
  (toggle-client
    (merge
      (toggle-scratchpad-app {:space-label "godot" :is-client? is-godot?})
      {:wsp->open-client
       (fn [{:as wsp}]
         (notify/notify "To do: open godot")
         (println wsp))})))
