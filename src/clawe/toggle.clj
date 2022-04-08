(ns clawe.toggle
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]

   [ralphie.notify :as notify]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   [ralphie.emacs :as r.emacs]
   [ralphie.spotify :as r.spotify]
   [ralphie.browser :as r.browser]

   [clawe.awesome :as c.awm] ;; DEPRECATED
   [clawe.defs.workspaces :as defs.workspaces]

   ;; READ fun comparison for later, i suppose
   [clawe.scratchpad :as scratchpad]

   ;; wonder what we need from here
   [clawe.workspaces :as workspaces]

   [ralphie.awesome :as awm]
   [ralphie.yabai :as yabai]))



(defn is-journal? [c]
  (or
    (and
      (-> c :awesome.client/class #{"Emacs"})
      (-> c :awesome.client/name #{"journal"}))
    (and
      (-> c :yabai.window/app #{"Emacs"})
      (-> c :yabai.window/title (#(re-seq #"^journal" %))))))

(defn is-web? [c]
  (or
    (-> c :awesome.client/class #{"Firefox"})
    (-> c :yabai.window/app #{"Safari"})))

(defn is-slack? [c]
  (or
    (-> c :awesome.client/class #{"Slack"})
    (-> c :yabai.window/app #{"Slack"})))

(defn is-spotify? [c]
  (or
    (-> c :awesome.client/class #{"Spotify"})
    (-> c :yabai.window/app #{"Spotify"})))


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
  [{:keys [wsp->client wsp->open-client clients->client client->hide client->show]}]
  (let [wsp    (workspaces/current-workspace)
        client (wsp->client wsp)
        client-focused
        (or
          (:yabai.window/has-focus client)
          (:awesome.client/focused client))]
    (cond
      ;; no tag, maybe create one?
      (not wsp)
      ;; TODO probably better to create the 'right' tag here if possible
      ;; maybe prompt for a tag name?
      (do
        (notify/notify "No current tag found...?" "Creating fallback")
        (awm/create-tag! "temp-tag")
        (awm/focus-tag! "temp-tag")
        (wsp->open-client nil))

      ;; client is focused, let's close/hide it
      (and client client-focused)
      (do
        (notify/notify "client found and focused")
        (cond
          client->hide
          (client->hide client)

          notify/is-mac?
          (yabai/close-window client)

          :else
          (awm/close-client client)))

      ;; client found in workspace, not focused
      (and client (not client-focused))
      (do
        (notify/notify "client found and not focused")
        (cond
          notify/is-mac?
          ;; TODO center and float? and bury?
          (yabai/focus-window client)

          :else
          ;; DEPRECATED
          (c.awm/focus-client {:center?   false
                               :float?    false
                               :bury-all? false} client)))

      ;; we have a current workspace, but no client in it
      ;; (not client)
      :else
      (let [client
            (when clients->client
              ;; TODO include awm clients (clawe.clients ns?)
              (clients->client (yabai/query-windows)))]

        (cond
          (and client client->show)
          (let []
            (notify/notify "found journal, ready to pull to this space!")
            (client->show client wsp))

          :else
          (do
            (wsp->open-client wsp)
            nil))))))

(comment
  (some->>
    (workspaces/current-workspace)
    workspaces/merge-awm-tags)
  )

;; TODO express godot/aseprite as a quick app-toggle right here
;; could be that it's a dried up version of the below toggle emacs/terminal
;; maybe just an :app/open workspace

(defkbd toggle-per-workspace-garden
  ;; TODO get garden files from workspace defs directly (rather than build them in here)
  [[:mod :shift] "g"]
  (toggle-client
    {:wsp->client
     (fn [{:workspace/keys [title] :awesome.tag/keys [clients]}]
       (let [wsp-garden-title (str "grdn-" title)]
         (->> clients (filter (comp #{wsp-garden-title} :awesome.client/name))
              first)))
     :wsp->open-client
     ;; TODO support the nil workspace case?
     (fn [{:workspace/keys [title]}]
       (let [wsp-garden-title (str "grdn-" title)]
         (r.emacs/open
           {:emacs.open/workspace wsp-garden-title
            :emacs.open/file
            (r.zsh/expand (str "~/todo/garden/workspaces/" title ".org"))})))}))

(defkbd toggle-terminal
  [[:mod] "Return"]
  (toggle-client
    {:wsp->client
     (fn [{:workspace/keys   [title]
           :awesome.tag/keys [clients]
           :yabai/keys       [windows]}]
       (cond
         (seq clients)
         (some->>
           clients
           (filter (fn [c]
                     (and
                       (-> c :awesome.client/class #{"Alacritty"})
                       (-> c :awesome.client/name #{title}))))
           first)

         (seq windows)
         (some->>
           windows
           (filter (fn [c]
                     ;; TODO check the title/tmux session?
                     (-> c :yabai.window/app #{"Alacritty"})))
           first)))
     :wsp->open-client
     (fn [{:workspace/keys [title directory]
           :git/keys       [repo]
           :as             wsp}]
       (if-not wsp
         (r.tmux/open-session)
         (let [directory (or directory repo (r.zsh/expand "~"))
               opts      {:tmux/session-name title :tmux/directory directory}]
           (r.tmux/open-session opts))))}))

(defkbd toggle-emacs
  [[:mod :shift] "Return"]
  (toggle-client
    {:wsp->client
     (fn [{:workspace/keys   [title]
           :awesome.tag/keys [clients]
           :yabai/keys       [windows]}]
       (cond
         (seq clients)
         (some->> clients
                  (filter (fn [c]
                            (and
                              (-> c :awesome.client/class #{"Emacs"})
                              (-> c :awesome.client/name #{title}))))
                  first)

         (seq windows)
         (some->> windows
                  (filter (fn [w]
                            ;; TODO one day check the title/emacs workspace
                            (-> w :yabai.window/app #{"Emacs"})))
                  first)))
     :wsp->open-client
     (fn [{:workspace/keys [title initial-file directory]
           :git/keys       [repo]
           :as             wsp}]
       (if-not wsp
         (r.emacs/open)
         (let [initial-file (or initial-file repo directory)
               opts         {:emacs.open/workspace title :emacs.open/file initial-file}]
           (r.emacs/open opts))))}))


(defn toggle-scratchpad-app [{:keys [space-name is-client?]}]
  {:wsp->client
   (fn [{:awesome.tag/keys [clients] :yabai/keys [windows]}]
     (some->> (concat clients windows) (filter is-client?) first))

   :clients->client
   (fn [clients] (some->> clients (filter is-client?) first))

   :client->hide
   (fn [c] (yabai/move-window-to-space c space-name))

   :client->show
   (fn [c wsp]
     (yabai/float-and-center-window c)
     (yabai/move-window-to-space c (or (:yabai.space/index wsp) (:workspace/title wsp)))
     ;; focus last so it doesn't move spaces on us
     (yabai/focus-window c))}
  )

(defcom toggle-journal-2
  (do
    (notify/notify "toggling-journal-2")
    (toggle-client
      (merge
        (toggle-scratchpad-app {:space-name "journal" :is-client? is-journal?})
        {:wsp->open-client
         (fn [{:workspace/keys [title initial-file directory]
               :git/keys       [repo]
               :as             wsp}]
           (let [opts { ;; TODO get from defworkspace journal
                       :emacs.open/workspace "journal"
                       :emacs.open/file      "/Users/russ/todo/journal.org"}]
             (r.emacs/open opts)))}))))


(defcom toggle-web-2
  (do
    (notify/notify "toggling-web-2")
    (toggle-client
      (merge
        (toggle-scratchpad-app {:space-name "web" :is-client? is-web?})
        {:wsp->open-client
         ;; TODO create the space if missing
         (r.browser/open)}))))

(defcom toggle-slack-2
  (do
    (notify/notify "toggling-slack-2")
    (toggle-client
      (merge
        (toggle-scratchpad-app {:space-name "slack" :is-client? is-slack?})
        {:wsp->open-client
         ;; TODO create the space if missing
         (fn [{:as wsp}]
           (notify/notify "To do: open slack")
           (println wsp))}))))

(defcom toggle-spotify-2
  (do
    (notify/notify "toggling-spotify-2")
    (toggle-client
      (merge
        (toggle-scratchpad-app {:space-name "spotify" :is-client? is-spotify?})
        {:wsp->open-client
         ;; TODO create the space if missing
         (fn [{:as wsp}]
           (notify/notify "To do: open spotify")
           (println wsp))}))))
