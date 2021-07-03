(ns clawe.defs.bindings
  (:require
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [chess.core :as chess]
   [defthing.defcom :as defcom]
   [ralphie.notify :as notify]
   [ralphie.rofi :as rofi]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   [ralphie.clipboard :as r.clip]
   [ralphie.screenshot :as r.screenshot]
   [ralphie.emacs :as r.emacs]
   [ralphie.awesome :as r.awm]
   [ralphie.spotify :as r.spotify]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.browser :as r.browser]
   [clawe.awesome :as awm]
   [clawe.bindings :refer [defkbd]]
   [clawe.defs.workspaces :as defs.workspaces]
   [clawe.defs.local.workspaces :as defs.local.workspaces]
   [clawe.dwim :as c.dwim]
   [clawe.scratchpad :as scratchpad]
   [clawe.workspaces :as workspaces]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi, launchers, command selectors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd clawe-rofi-sxhkd
  [[:mod] "w"]
  ;; {:binding/sxhkd true}
  (defcom/exec c.dwim/dwim))

(defkbd clawe-rofi-awm
  [[:mod] "x"]
  {:binding/awm true}
  ;; {:binding/awm true}
  (defcom/exec c.dwim/dwim))

(defkbd rofi-launcher
  [[:mod] "space"]
  (awm/awm-fnl '(awful.spawn.easy_async "rofi -show combi")))

(defkbd kill-client
  [[:mod] "q"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (c:kill))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd screenshot-full
  [[:mod :shift] "s"]
  (r.screenshot/full-screen))

(defkbd screenshot-region
  [[:mod :shift] "a"]
  (r.screenshot/select-region))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd uuid-on-clipboard
  [[:mod :ctrl] "u"]
  (let [uuid (str (java.util.UUID/randomUUID))]
    (notify/notify "clippy!")
    (r.clip/set-clip uuid)))

(comment
  (->>
    (defthing.defcom/list-commands)
    (filter :name)
    (filter (fn [com] (-> com :name (#(string/includes? % "uuid-on")))))
    ;; first
    ;; defthing.defcom/exec
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Titlebars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd toggle-all-titlebars
  [[:mod :shift] "t"]
  "checks the first client to see if titlebars are visible or not"
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
             (lume.each awful.titlebar.hide)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Window layout
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd toggle-floating
  [[:mod] "f"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (if c.floating
         (tset c :ontop false)
         (tset c :ontop true))
       (awful.client.floating.toggle c))))

(defkbd bury-all-windows
  [[:mod :shift] "f"]
  ;; tho being smart enough to consume this would be fun too
  (awm/awm-fnl
    '(->
       (client.get)
       (lume.each (fn [c] (tset c :floating false))))))

(defkbd swap-master
  [[:mod :ctrl] "Return"]
  (awm/awm-fnl
    '(let [c _G.client.focus
           m (awful.client.getmaster)]
       (if (= c.window m.window)
         ;; if we are master, swap us back
         (awful.client.setslave c)
         (c:swap m)))))

(defkbd center-window
  [[:mod] "c"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (tset c :ontop true)
       (tset c :floating true)
       (-> c
           ((+ awful.placement.scale
               awful.placement.centered)
            {:honor_padding  true
             :honor_workarea true
             :to_percent     0.75})))))

(defkbd center-window-large
  [[:mod :shift] "c"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (tset c :ontop true)
       (tset c :floating true)
       (-> c
           ((+ awful.placement.scale
               awful.placement.centered)
            {:honor_padding  true
             :honor_workarea true
             :to_percent     0.9})))))

(defkbd center-window-small
  [[:mod] "v"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (tset c :ontop true)
       (tset c :floating true)
       (-> c
           ((+ awful.placement.scale
               awful.placement.centered)
            {:honor_padding  true
             :honor_workarea true
             :to_percent     0.5})))))

(defkbd center-window-no-resize
  [[:mod :ctrl] "c"]
  (awm/awm-fnl
    '(let [c _G.client.focus]
       (tset c :ontop true)
       (tset c :floating true)
       (-> c
           (awful.placement.centered
             {:honor_padding  true
              :honor_workarea true})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; chess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd open-chess-game
  [[:mod :shift] "e"]
  (do
    (notify/notify "Fetching chess games")
    (->>
      (chess/fetch-games)
      (map (fn [{:keys [white-user black-user] :as game}]
             (assoc game
                    :rofi/label (str white-user " vs " black-user))))
      (rofi/rofi {:msg       "Open Game"
                  :on-select (fn [{:keys [lichess/url]}]
                               (notify/notify "Opening game" url)
                               (r.browser/open {:browser.open/url url}))}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Notifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reload-notification-css []
  (process/$
    notify-send.py a --hint boolean:deadd-notification-center:true
    string:type:reloadStyle))

(defkbd toggle-notifications-center
  [[:mod :alt] "n"]
  ;; (reload-notification-css)
  (let [deadd-pid (->
                    ^{:out :string}
                    (process/$ pidof deadd-notification-center)
                    process/check
                    :out
                    string/trim)]
    (-> (process/$ kill -s USR1 ~deadd-pid)
        process/check)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace toggling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle-workspace [workspace]
  (-> workspace
      workspaces/merge-awm-tags
      scratchpad/toggle-scratchpad))

(comment
  (-> defs.local.workspaces/editor toggle-workspace))

;; these should come for free, with :binding/scratchpad options
(defkbd toggle-workspace-journal
  [[:mod] "u"]
  (toggle-workspace defs.workspaces/journal))

(defkbd toggle-workspace-web
  [[:mod] "t"]
  (toggle-workspace defs.workspaces/web)
  ;; (toggle-workspace defs.workspaces/dev-browser)
  )

(defkbd toggle-workspace-chrome-browser
  [[:mod] "b"]
  (toggle-workspace defs.workspaces/dev-browser))

(defkbd toggle-workspace-slack
  [[:mod] "a"]
  (toggle-workspace defs.workspaces/slack))

(defkbd toggle-workspace-spotify
  [[:mod] "s"]
  (toggle-workspace defs.workspaces/spotify))

(defkbd toggle-workspace-godot
  [[:mod] "g"]
  (toggle-workspace defs.workspaces/godot))

(defkbd toggle-workspace-zoom
  [[:mod] "z"]
  (toggle-workspace defs.workspaces/zoom))

(defkbd toggle-workspace-one-password
  [[:mod] "."]
  (fn [_ _] (toggle-workspace defs.workspaces/one-password)))

(defkbd toggle-workspace-pixels
  [[:mod :shift] "p"]
  (fn [_ _] (toggle-workspace defs.workspaces/pixels)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App toggling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO express godot/aseprite as a quick app-toggle right here
;; could be that it's a dried up version of the below toggle emacs/terminal
;; maybe just an :app/open workspace
(defkbd toggle-per-workspace-garden
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

(defkbd toggle-terminal
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

(defkbd toggle-emacs
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

(defkbd cycle-prev-tag
  [[:mod] "Left"]
  (awm/awm-fnl '(awful.tag.viewprev)))

(defkbd cycle-next-tag
  [[:mod] "Right"]
  (awm/awm-fnl '(awful.tag.viewnext)))

(defkbd cycle-prev-tag-2
  [[:mod] "n"]
  (awm/awm-fnl '(awful.tag.viewprev)))

(defkbd cycle-next-tag-2
  [[:mod] "p"]
  (awm/awm-fnl '(awful.tag.viewnext)))

(defkbd drag-workspace-prev
  [[:mod :shift] "Left"]
  (workspaces/drag-workspace "down"))

(defkbd drag-workspace-next
  [[:mod :shift] "Right"]
  (workspaces/drag-workspace "up"))

(defkbd clean-workspaces
  [[:mod] "d"]
  (do
    (workspaces/clean-workspaces)
    (workspaces/consolidate-workspaces)
    (workspaces/update-workspaces-widget)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Brightness
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd brightness-up
  [[] "XF86MonBrightnessUp"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "light -A 5")))

(defkbd brightness-down
  [[] "XF86MonBrightnessDown"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "light -U 5")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Play/Pause/Next/Prev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd spotify-pause
  [[] "XF86AudioPause"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "spotifycli --playpause")))

(defkbd spotify-play
  [[] "XF86AudioPlay"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "spotifycli --playpause")))

(defkbd audio-next
  [[] "XF86AudioNext"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "playerctl next")))

(defkbd audio-prev
  [[] "XF86AudioPrev"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "playerctl previous")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muting input/output
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd toggle-input-mute
  [[:mod] "m"]
  ;; TODO write another supported wrapper/unwrapper that lets me include clawe-based notifs here
  ;; could even still write it's own defcom, and fire both the raw-fnl and clojure command
  (do
    (->
      ($ amixer set Capture toggle)
      check :out slurp)
    (notify/notify
      {:notify/subject "Mute Toggled!"
       :notify/body    (if (r.pulseaudio/input-muted?)
                         "Muted!" "Unmuted!")
       :notify/id      "mute-notif"})))

(defkbd toggle-output-mute
  [[] "XF86AudioMute"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "pactl set-sink-mute @DEFAULT_SINK@ toggle")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Volume up/down
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (notify/notify {:notify/subject "Current volume"
                  :notify/body    (r.pulseaudio/default-sink-volume)
                  :notify/id      "volume"}))

(defkbd volume-up
  [[] "XF86AudioRaiseVolume"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "pactl set-sink-volume @DEFAULT_SINK@ +5%")))

(defkbd volume-down
  [[] "XF86AudioLowerVolume"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "pactl set-sink-volume @DEFAULT_SINK@ -5%")))

(defkbd spotify-volume-up
  [[:mod] "XF86AudioRaiseVolume"]
  (r.spotify/adjust-spotify-volume "up"))

(defkbd spotify-volume-down
  [[:mod] "XF86AudioLowerVolume"]
  (r.spotify/adjust-spotify-volume "down"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open Workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd open-workspace
  [[:mod] "o"]
  (do
    (notify/notify "Opening Workspace!")
    (workspaces/open-workspace)))

(defkbd create-new-workspace
  [[:mod :shift] "o"]
  ;; TODO add support for creating a new one
  (notify/notify "Creating new Workspace!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cycle focus
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd cycle-focus
  [[:mod] "e"]
  ;; TODO impl an actual cycle - right now it just focuses the first
  ;; client on screen that isn't focused
  (awm/awm-fnl
    '(let [c (->
               (awful.screen.focused)
               (. :clients)
               (lume.reject (fn [c] (= c.window client.focus.window)))
               (lume.first))]
       (set _G.client.focus c))))

(defkbd cycle-layout-next
  [[:mod] "Tab"]
  (awm/awm-fnl
    '(let [scr (awful.screen.focused)]
       (awful.layout.inc 1 scr _G.layouts))))

(defkbd cycle-layout-prev
  [[:mod :shift] "Tab"]
  (awm/awm-fnl
    '(let [scr (awful.screen.focused)]
       (awful.layout.inc -1 scr _G.layouts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (set exp.clientkeys
;;      (gears.table.join
;;       ;; kill current client

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
