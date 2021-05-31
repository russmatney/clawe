(ns clawe.defs.bindings
  (:require
   [clawe.defthing :as defthing]
   [clawe.awesome :as awm]
   [clawe.workspaces :as workspaces]
   [clawe.defs.workspaces :as defs.workspaces]
   [ralph.defcom :as defcom]
   [ralphie.notify :as notify]
   [ralphie.rofi :as rofi]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   [ralphie.clipboard :as r.clip]
   ;; TODO remove non bb deps from chess (clj-http)
   [chess.core :as chess]
   ;; TODO require as first class dep
   [systemic.core :as sys]

   [clawe.defs.local.workspaces :as defs.local.workspaces]

   [clojure.string :as string]
   [babashka.process :as process :refer [$ check]]
   [ralphie.emacs :as r.emacs]
   [ralphie.awesome :as r.awm]
   [ralphie.spotify :as r.spotify]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.browser :as r.browser]
   [clawe.scratchpad :as scratchpad]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-bindings []
  (defthing/list-xs :clawe/bindings))

(defn get-binding [bd]
  (defthing/get-x :clawe/bindings
    (comp #{(:name bd bd)} :name)))

(defn binding-key->str [k]
  (let [mods (->> (first k) (apply str) (#(string/replace % #":" "")))
        k (second k)]
    (str mods k)))

(defn binding->defcom
  "Expands a binding to fulfill the defcom api."
  [{:keys [binding/key binding/command name] :as bd}]
  (let [nm (str name "-keybdg-" (binding-key->str key))
        bd (assoc bd
                  :defcom/name nm
                  :defcom/handler command
                  ;; an attempt to deal with defcom's 2 airty situation
                  ;; (fn [_ _] (command))
                  )]
    ;; not sure if this is enough - might fail in some cases (install-micro)
    (defcom/add-command (symbol nm) bd)
    bd))

(defmacro defbinding [title & args]
  (apply defthing/defthing :clawe/bindings title args))


(declare kbd)
(defmacro defbinding-kbd [title key-def command]
  `(defbinding ~title
    (kbd ~key-def ~command)
    binding->defcom))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Binding helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kbd [key command]
  (fn [x]
    (assoc x
           :binding/key key
           :binding/command command)))

(defn call-kbd [bd]
  ((:binding/command bd) nil nil))

(defbinding-kbd uuid-on-clipboard
  [[:mod :ctrl] "u"]
  (fn [_ _]
    (let [uuid (str (java.util.UUID/randomUUID))]
      (r.clip/set-clip uuid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Titlebars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd toggle-all-titlebars
  [[:mod :shift] "t"]
  (fn [_ _]
    ;; checks the first client to see if titlebars are visible or not
    (let [no-titlebar
          (awm/awm-fnl '(->
                          (client.get)
                          (lume.first)
                          ((fn [c]
                             (let [(_ size) (c:titlebar_top)]
                               (= size 0))))
                          (view)))]
      (if no-titlebar
        (do
          (notify/notify "Showing all titlebars")
          (awm/awm-fnl
            '(->
               (client.get)
               (lume.each awful.titlebar.show))))
        (do
          (notify/notify "Hiding all titlebars")
          (awm/awm-fnl
            '(->
               (client.get)
               (lume.each awful.titlebar.hide))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Window layout
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd bury-all-windows
  [[:mod :shift] "f"]
  (fn [_ _]
    (notify/notify "burying all windows")
    (awm/awm-fnl
      '(->
         (client.get)
         (lume.each (fn [c] (tset c :floating false)))))))

(defbinding-kbd center-window-small
  [[:mod] "v"]
  (fn [_ _]
    (notify/notify "center-window-small")
    ;; TODO impl awesome client-style bindings?
    ;;
    (awm/awm-fnl
      '(let [c (awful.focused.client)]
         (tset c :ontop true)
         (tset :floating true)
         (-> c
             ((+ awful.placement.scale
                 awful.placement.centered)
              {:honor_padding  true
               :honor_workarea true
               :to_percent     0.5}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; chess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd open-chess-game
  [[:mod :shift] "e"]
  (fn [_ _]
    (notify/notify "Fetching chess games")
    (->>
      (chess/fetch-games)
      (map (fn [{:keys [lichess/url white-user black-user] :as game}]
             (assoc game
                    :rofi/label (str white-user " vs " black-user))))
      (rofi/rofi {:msg       "Open Game"
                  :on-select (fn [{:keys [lichess/url]}]
                               (notify/notify "Opening game" url)
                               (r.browser/open {:browser.open/url url}))}))))

(comment
  (chess/fetch-games)
  (call-kbd open-chess-game))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Notifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reload-notification-css []
  (process/$
    notify-send.py a --hint boolean:deadd-notification-center:true
    string:type:reloadStyle))

(defbinding-kbd toggle-notifications-center
  [[:mod :alt] "n"]
  (fn [_ _]
    ;; (reload-notification-css)
    (let [deadd-pid (->
                      ^{:out :string}
                      (process/$ pidof deadd-notification-center)
                      process/check
                      :out
                      string/trim)]
      (-> (process/$ kill -s USR1 ~deadd-pid)
          process/check))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace toggling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle-workspace-with-name [name]
  (-> name
      defs.workspaces/get-workspace
      workspaces/merge-awm-tags
      scratchpad/toggle-scratchpad))

(defn toggle-workspace [workspace]
  (-> workspace
      workspaces/merge-awm-tags
      scratchpad/toggle-scratchpad))

(comment
  (toggle-workspace-with-name "journal")
  (-> "spotify"
      defs.workspaces/get-workspace
      workspaces/merge-awm-tags
      :awesome/clients)

  (-> defs.local.workspaces/editor
      toggle-workspace)
  )

;; TODO import and depend on defs/workspace? or is that backwards?
;; These should come for free, with :bindings/scratchpad options
(defbinding-kbd toggle-workspace-journal
  [[:mod] "u"]
  (fn [_ _] (toggle-workspace defs.workspaces/journal)))

;; (defbinding-kbd toggle-workspace-editor
;;   [[:mod] "e"]
;;   (fn [_ _] (toggle-workspace defs.local.workspaces/editor)))

(defbinding-kbd toggle-workspace-web
  [[:mod] "t"]
  (fn [_ _]
    ;; TODO configuration system? or a go-to-def system?
    ;; (toggle-workspace defs.workspaces/web)
    (toggle-workspace defs.workspaces/dev-browser)))

(defbinding-kbd toggle-workspace-chrome-browser
  [[:mod] "b"]
  (fn [_ _] (toggle-workspace defs.workspaces/dev-browser)))

(defbinding-kbd toggle-workspace-slack
  [[:mod] "a"]
  (fn [_ _] (toggle-workspace defs.workspaces/slack)))

(defbinding-kbd toggle-workspace-spotify
  [[:mod] "s"]
  (fn [_ _] (toggle-workspace defs.workspaces/spotify)))

(defbinding-kbd toggle-workspace-godot
  [[:mod] "g"]
  (fn [_ _] (toggle-workspace defs.workspaces/godot)))

(defbinding-kbd toggle-workspace-zoom
  [[:mod] "z"]
  (fn [_ _] (toggle-workspace defs.workspaces/zoom)))

(defbinding-kbd toggle-workspace-one-password
  [[:mod] "."]
  (fn [_ _] (toggle-workspace defs.workspaces/one-password)))

(defbinding-kbd toggle-workspace-pixels
  [[:mod :shift] "p"]
  (fn [_ _] (toggle-workspace defs.workspaces/pixels)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App toggling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO express godot/aseprite as a quick app-toggle right here
;; could be that it's a dried up version of the below toggle emacs/terminal
;; maybe just an :app/open workspace
(defbinding-kbd toggle-per-workspace-garden
  [[:mod :shift] "g"]
  (fn [_ _]
    (notify/notify "Toggling per-workspace garden")
    (let [{:workspace/keys [title]
           :awesome/keys   [clients]}
          (some->> [(workspaces/current-workspace)]
                   (workspaces/merge-awm-tags)
                   first)
          wsp-garden-title (str "garden-" title)
          open-client
          (->> clients (filter (comp #{wsp-garden-title} :name))
               first)]
      (cond
        (and open-client (:focused open-client))
        (awm/close-client open-client)
        (and open-client (not (:focused open-client)))
        (awm/focus-client open-client)
        (not open-client)
        (r.emacs/open
          {:emacs.open/workspace wsp-garden-title
           :emacs.open/file
           (r.zsh/expand (str "~/todo/garden/workspaces/" title ".org"))})))))

(defbinding-kbd toggle-terminal
  [[:mod] "Return"]
  (fn [_ _]
    (let [{:workspace/keys [title directory]
           :git/keys       [repo]
           :awesome/keys   [clients]}
          (some->> (workspaces/current-workspace)
                   workspaces/merge-awm-tags)
          directory               (or directory repo (r.zsh/expand "~"))
          terminal-client         (some->> clients
                                           (filter (fn [c]
                                                     (and
                                                       (-> c :class #{"Alacritty"})
                                                       (-> c :name #{title}))))
                                           first)
          terminal-client-focused (:focused terminal-client)

          opts {:tmux/name      title
                :tmux/directory directory}]
      (notify/notify "Toggling Terminal"
                     (assoc opts
                            :terminal-client terminal-client
                            :already-focused terminal-client-focused))
      (cond
        ;; no tag
        (not title)
        (do
          (r.awm/create-tag! "temp-tag")
          (r.awm/focus-tag! "temp-tag")
          (r.tmux/open-session))

        (and terminal-client terminal-client-focused)
        (awm/close-client terminal-client)

        (and terminal-client (not terminal-client-focused))
        (awm/focus-client {:center?   false
                           :float?    false
                           :bury-all? false} terminal-client)

        :else
        (do (r.tmux/open-session opts)
            nil)))))

(defbinding-kbd toggle-emacs
  [[:mod :shift] "Return"]
  (fn [_ _]
    (let [
          {:workspace/keys [title initial-file directory]
           :git/keys       [repo]
           :awesome/keys   [clients]}
          (some->> (workspaces/current-workspace)
                   workspaces/merge-awm-tags)
          emacs-client         (some->> clients
                                        (filter (fn [c]
                                                  (and
                                                    (-> c :class #{"Emacs"})
                                                    (-> c :name #{title}))))
                                        first)
          emacs-client-focused (:focused emacs-client)
          initial-file         (or
                                 ;; TODO detect if configured file exists
                                 initial-file
                                 repo
                                 directory)
          opts
          {:emacs.open/workspace title
           ;; TODO only set the file for a new emacs workspaces
           :emacs.open/file      initial-file}]
      (notify/notify "Toggling Emacs"
                     (assoc opts
                            :emacs-client emacs-client
                            :already-focused emacs-client-focused))
      (cond
        ;; no tag
        (not title)
        (do
          (r.awm/create-tag! "temp-tag")
          (r.awm/focus-tag! "temp-tag")
          (r.emacs/open))

        (and emacs-client emacs-client-focused)
        (awm/close-client emacs-client)

        (and emacs-client (not emacs-client-focused))
        (awm/focus-client {:center?   false
                           :float?    false
                           :bury-all? false} emacs-client)

        :else
        (do
          (r.emacs/open opts)
          nil)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cycle workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd cycle-prev-tag
  [[:mod] "Left"]
  (fn [_ _] (awm/awm-fnl '(awful.tag.viewprev))))

(defbinding-kbd cycle-next-tag
  [[:mod] "Right"]
  (fn [_ _] (awm/awm-fnl '(awful.tag.viewnext))))

(defbinding-kbd cycle-prev-tag
  [[:mod] "n"]
  (fn [_ _] (awm/awm-fnl '(awful.tag.viewprev))))

(defbinding-kbd cycle-next-tag
  [[:mod] "p"]
  (fn [_ _] (awm/awm-fnl '(awful.tag.viewnext))))

(defbinding-kbd drag-workspace-prev
  [[:mod :shift] "Left"]
  (fn [_ _] (workspaces/drag-workspace "down")))

(defbinding-kbd drag-workspace-next
  [[:mod :shift] "Right"]
  (fn [_ _] (workspaces/drag-workspace "up")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Brightness
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd brightness-up
  [[] "XF86MonBrightnessUp"]
  (fn [_ _]
    (->
      ($ light -A 5)
      check :out slurp)))

(defbinding-kbd brightness-down
  [[] "XF86MonBrightnessDown"]
  (fn [_ _]
    (->
      ($ light -U 5)
      check :out slurp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Play/Pause/Next/Prev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd spotify-pause
  [[] "XF86AudioPause"]
  (fn [_ _]
    (->
      ($ spotifycli --playpause)
      check :out slurp)))

(defbinding-kbd spotify-play
  [[] "XF86AudioPlay"]
  (fn [_ _]
    (->
      ($ spotifycli --playpause)
      check :out slurp)))

(defbinding-kbd audio-next
  [[] "XF86AudioNext"]
  (fn [_ _]
    (->
      ($ playerctl next)
      check :out slurp)))

(defbinding-kbd audio-prev
  [[] "XF86AudioPrev"]
  (fn [_ _]
    (->
      ($ playerctl previous)
      check :out slurp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muting input/output
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd toggle-input-mute
  [[:mod] "m"]
  (fn [_ _]
    (->
      ($ amixer set Capture toggle)
      check :out slurp)
    (notify/notify
      {:notify/subject "Mute Toggled!"
       :notify/body    (if (r.pulseaudio/input-muted?)
                         "Muted!" "Unmuted!")
       :notify/id      "mute-notif"})))

(defbinding-kbd toggle-output-mute
  [[] "XF86AudioMute"]
  (fn [_ _]
    (->
      ($ pactl set-sink-mute "@DEFAULT_SINK@" toggle)
      check :out slurp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Volume up/down
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd volume-up
  [[] "XF86AudioRaiseVolume"]
  (fn [_ _]
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "+5%")
      check :out slurp)
    (notify/notify {:notify/subject "Raised volume"
                    :notify/body (r.pulseaudio/default-sink-volume)
                    :notify/id "changed-volume"})))

(defbinding-kbd volume-down
  [[] "XF86AudioLowerVolume"]
  (fn [_ _]
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "-5%")
      check :out slurp)
    (notify/notify {:notify/subject "Lowered volume"
                    :notify/body (r.pulseaudio/default-sink-volume)
                    :notify/id "changed-volume"})))

(defbinding-kbd spotify-volume-up
  [[:mod] "XF86AudioRaiseVolume"]
  (fn [_ _]
    ;; TODO this api is nonsense, should refactor when defcom arity is fixed
    (r.spotify/spotify-volume nil {:arguments ["up"]})))

(defbinding-kbd spotify-volume-down
  [[:mod] "XF86AudioLowerVolume"]
  (fn [_ _]
    ;; TODO this api is nonsense, should refactor when defcom arity is fixed
    (r.spotify/spotify-volume nil {:arguments ["down"]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open Workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd open-workspace
  [[:mod] "o"]
  (fn [_ _]
    (notify/notify "Opening Workspace!")
    (workspaces/open-workspace)))

(defbinding-kbd create-new-workspace
  [[:mod :shift] "o"]
  (fn [_ _]
    ;; TODO add support for creating a new one
    (notify/notify "Creating new Workspace!")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cycle focus
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd cycle-focus
  [[:mod] "e"]
  (fn [_ _]
    (notify/notify "Cycling focus")
    ;; TODO impl an actual cycle - right now it just focuses the first
    ;; client on screen that isn't focused
    (awm/awm-fnl
      '(let [c (->
                 (awful.screen.focused)
                 (. :clients)
                 (lume.reject (fn [c] (= c.window client.focus.window)))
                 (lume.first))]
         (set _G.client.focus c)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Not yet transcribed
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;    ;; TODO move all these bindings into ralphie itself
;;    ;; ralphie rofi
;;    (key [:mod] "x" (spawn-fn "ralphie rofi"))

;;    ;; clawe keybindings
;;    (key [:mod] "r"
;;         (fn []
;;           ;; WARN potential race case on widgets reloading
;;           (awful.spawn "clawe rebuild-clawe" false)
;;           (awful.spawn "clawe reload" false)))

;;    (key [:mod] "d" (spawn-fn "clawe clean-workspaces"))
;;    (key [:mod] "w" (spawn-fn "clawe rofi"))

;;    ;; cycle layouts
;;    (key [:mod] "Tab"
;;         (fn []
;;           (let [scr (awful.screen.focused)]
;;             (awful.layout.inc 1 scr _G.layouts))))
;;    (key [:mod :shift] "Tab"
;;         (fn []
;;           (let [scr (awful.screen.focused)]
;;             (awful.layout.inc -1 scr _G.layouts))))

;;    ;; launcher (rofi)
;;    (key [:mod] "space" (spawn-fn "/usr/bin/rofi -show drun -modi drun"))

;;    ;; finder (thunar)
;;    (key [:mod] "e" (spawn-fn "/usr/bin/thunar"))

;;    ;; screenshots
;;    (key [:mod :shift] "s" (spawn-fn "ralphie screenshot full"))
;;    (key [:mod :shift] "a" (spawn-fn "ralphie screenshot region"))

;;    ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (set exp.clientkeys
;;      (gears.table.join
;;       ;; kill current client
;;       (key [:mod] "q" (fn [c] (c:kill)))

;;       ;; toggle floating
;;       (key [:mod] "f" (fn [c]
;;                         (if c.floating
;;                             (tset c :ontop false)
;;                             (tset c :ontop true))
;;                         (awful.client.floating.toggle c)))

;;       ;; focus movement
;;       (key [:mod :shift] "l" (fn [c]
;;                                (if c.floating
;;                                    (move-client c "right")
;;                                    (focus-move "right" "right" "up"))))
;;       (key [:mod :shift] "h" (fn [c]
;;                                (if c.floating
;;                                    (move-client c "left")
;;                                    (focus-move "left" "left" "down"))))
;;       (key [:mod :shift] "j" (fn [c]
;;                                (if c.floating
;;                                    (move-client c "down")
;;                                    (focus-move "down" "right" "down"))))
;;       (key [:mod :shift] "k" (fn [c]
;;                                (if c.floating
;;                                    (move-client c "up")
;;                                    (focus-move "up" "left" "up"))))

;;       ;; widen/shink windows
;;       (key [:ctrl :shift] "l"
;;            (fn [c]
;;              (if c.floating
;;                  (do
;;                    (awful.placement.scale
;;                     c {:direction "right"
;;                        :by_percent 1.1})
;;                    (awful.placement.scale
;;                     c {:direction "left"
;;                        :by_percent 1.1})
;;                    )
;;                  (awful.tag.incmwfact 0.05))))
;;       (key [:ctrl :shift] "h"
;;            (fn [c]
;;              (if c.floating
;;                  (do
;;                    (awful.placement.scale
;;                     c {:direction "left"
;;                        :by_percent 0.9})
;;                    (awful.placement.scale
;;                     c {:direction "right"
;;                        :by_percent 0.9})
;;                    )
;;                  (awful.tag.incmwfact -0.05))))
;;       (key [:ctrl :shift] "j"
;;            (fn [c]
;;              (if c.floating
;;                  (do
;;                    (awful.placement.scale
;;                     c {:direction "down"
;;                        :by_percent 1.1})
;;                    (awful.placement.scale
;;                     c {:direction "up"
;;                        :by_percent 1.1})
;;                    )
;;                  (awful.client.incwfact 0.05))))
;;       (key [:ctrl :shift] "k"
;;            (fn [c]
;;              (if c.floating
;;                  (do
;;                    (awful.placement.scale
;;                     c {:direction "up"
;;                        :by_percent 0.9})
;;                    (awful.placement.scale
;;                     c {:direction "down"
;;                        :by_percent 0.9})
;;                    )
;;                  (awful.client.incwfact -0.05))))

;;       ;; center on screen
;;       (key [:mod] "c"
;;            (fn [c]
;;              (tset c :ontop true)
;;              (-> c
;;                  (tset :floating true)
;;                  ((+ awful.placement.scale
;;                      awful.placement.centered)
;;                   {:honor_padding true
;;                    :honor_workarea true
;;                    :to_percent 0.75}))))

;;       ;; large centered
;;       (key [:mod :shift] "c"
;;            (fn [c]
;;              (-> c
;;                  (tset :floating true)
;;                  ((+ awful.placement.scale
;;                      awful.placement.centered)
;;                   {:honor_padding true
;;                    :honor_workarea true
;;                    :to_percent 0.9}))))

;;       ;; center without resizing
;;       (key [:mod :ctrl] "c"
;;            (fn [c]
;;              (-> c
;;                  (tset :floating true)
;;                  (awful.placement.centered
;;                   {:honor_padding true
;;                    :honor_workarea true}))))

;;       ;; swap with master
;;       (key [:mod :ctrl] "Return" (fn [c] (c:swap (awful.client.getmaster))))))
