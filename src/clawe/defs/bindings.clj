(ns clawe.defs.bindings
  (:require
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [chess.core :as chess]
   [defthing.defcom :as defcom :refer [defcom]]
   [defthing.defkbd :refer [defkbd]]

   [ralphie.notify :as notify]
   [ralphie.rofi :as rofi]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   [ralphie.clipboard :as r.clip]
   [ralphie.screenshot :as r.screenshot]
   [ralphie.emacs :as r.emacs]
   [ralphie.awesome :as awm]
   [ralphie.spotify :as r.spotify]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.browser :as r.browser]

   [clawe.awesome :as c.awm] ;; DEPRECATED
   [clawe.defs.workspaces :as defs.workspaces]
   [clawe.defs.local.workspaces :as defs.local.workspaces]
   [clawe.dwim :as c.dwim]
   [clawe.scratchpad :as scratchpad]
   [clawe.workspaces :as workspaces]
   [clawe.rules :as c.rules]))

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
  (do
    (r.screenshot/full-screen)
    (slurp "http://localhost:3334/screenshots/update")))

(defkbd screenshot-region
  [[:mod :shift] "a"]
  (do
    (r.screenshot/select-region)
    (slurp "http://localhost:3334/screenshots/update")))


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
               awful.placement.centered
               awful.placement.maximize_vertically)
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

(defcom open-chess-game
  ;; [[:mod :shift] "e"]
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
      scratchpad/toggle-scratchpad)
  (slurp "http://localhost:3334/dock/update")
  )

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
    (let [{:workspace/keys   [title]
           :awesome.tag/keys [clients]}
          (some->> [(workspaces/current-workspace)]
                   (workspaces/merge-awm-tags)
                   first)
          wsp-garden-title (str "garden-" title)
          open-client
          (->> clients (filter (comp #{wsp-garden-title} :awesome.client/name))
               first)]
      (cond
        (and open-client (:awesome.client/focused open-client))
        (awm/close-client open-client)
        (and open-client (not (:awesome.client/focused open-client)))
        ;; DEPRECATED
        (c.awm/focus-client open-client)
        (not open-client)
        (r.emacs/open
          {:emacs.open/workspace wsp-garden-title
           :emacs.open/file
           (r.zsh/expand (str "~/todo/garden/workspaces/" title ".org"))})))))

(defkbd toggle-terminal
  [[:mod] "Return"]
  (let [{:workspace/keys   [title directory]
         :git/keys         [repo]
         :awesome.tag/keys [clients]}
        (some->> (workspaces/current-workspace)
                 workspaces/merge-awm-tags)
        directory               (or directory repo (r.zsh/expand "~"))
        terminal-client         (some->> clients
                                         (filter (fn [c]
                                                   (and
                                                     (-> c :awesome.client/class #{"Alacritty"})
                                                     (-> c :awesome.client/name #{title}))))
                                         first)
        terminal-client-focused (:awesome.client/focused terminal-client)

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
        (awm/create-tag! "temp-tag")
        (awm/focus-tag! "temp-tag")
        (r.tmux/open-session))

      (and terminal-client terminal-client-focused)
      (awm/close-client terminal-client)

      (and terminal-client (not terminal-client-focused))
      ;; DEPRECATED
      (c.awm/focus-client {:center?   false
                           :float?    false
                           :bury-all? false} terminal-client)

      :else
      (do (r.tmux/open-session opts)
          nil))))

(defkbd toggle-emacs
  [[:mod :shift] "Return"]
  (let [{:workspace/keys   [title initial-file directory]
         :git/keys         [repo]
         :awesome.tag/keys [clients]}
        (some->> (workspaces/current-workspace)
                 workspaces/merge-awm-tags)
        emacs-client         (some->> clients
                                      (filter (fn [c]
                                                (and
                                                  (-> c :awesome.client/class #{"Emacs"})
                                                  (-> c :awesome.client/name #{title}))))
                                      first)
        emacs-client-focused (:awesome.client/focused emacs-client)
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
        (println "no tag, creating")
        (awm/create-tag! "temp-tag")
        (awm/focus-tag! "temp-tag")
        (r.emacs/open))

      (and emacs-client emacs-client-focused)
      (do
        (println "client is focused, closing")
        (awm/close-client emacs-client))

      (and emacs-client (not emacs-client-focused))
      (do
        (println "client not focused, focusing")
        ;; DEPRECATED
        (c.awm/focus-client {:center?   false
                             :float?    false
                             :bury-all? false} emacs-client))

      :else
      (do
        (println "no emacs-client in current tag, opening new one")
        (r.emacs/open opts)
        nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cycle tags and clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd cycle-prev-tag
  [[:mod] "Left"]
  (awm/awm-fnl '(awful.tag.viewprev)))

(defkbd cycle-next-tag
  [[:mod] "Right"]
  (awm/awm-fnl '(awful.tag.viewnext)))

(defkbd cycle-next-tag-2
  [[:mod] "Tab"]
  (awm/awm-fnl
    '(do (awful.tag.viewnext)
         (awful.spawn.easy_async "curl http://localhost:3334/dock/update" nil))))

(defkbd cycle-prev-tag-2
  [[:mod :shift] "Tab"]
  (awm/awm-fnl
    '(do (awful.tag.viewprev)
         (awful.spawn.easy_async "curl http://localhost:3334/dock/update" nil))))

(defkbd cycle-focus-next
  [[:mod] "n"]
  (awm/awm-fnl '(awful.client.focus.byidx 1))
  ;; (do
  ;;   ;; TODO should be able to notify from sxhkd, but run this awm-fnl from awesome
  ;;   (notify/notify "Client focus next!" (seq (awm/visible-clients))))
  )

(defkbd cycle-focus-prev
  [[:mod] "p"]
  (awm/awm-fnl '(awful.client.focus.byidx -1))
  ;; (do
  ;;   (notify/notify "Client focus prev!" (seq (awm/visible-clients))))
  )

(defkbd cycle-layout-next
  [[:mod] "e"]
  (awm/awm-fnl
    '(let [scr (awful.screen.focused)]
       (awful.layout.inc 1 scr _G.layouts))))

(defkbd cycle-layout-prev
  [[:mod :shift] "e"]
  (awm/awm-fnl
    '(let [scr (awful.screen.focused)]
       (awful.layout.inc -1 scr _G.layouts))))

(defkbd drag-workspace-prev
  [[:mod :shift] "Left"]
  (do
    (workspaces/drag-workspace "down")
    (slurp "http://localhost:3334/dock/update"))
  )

(defkbd drag-workspace-next
  [[:mod :shift] "Right"]
  (do
    (workspaces/drag-workspace "up")
    (slurp "http://localhost:3334/dock/update"))
  )

(defkbd correct-clients-and-workspaces
  [[:mod] "d"]
  "Applies clawe rules"
  (c.rules/correct-clients-and-workspaces))

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
       :notify/id      "mute-notif"})
    (slurp "http://localhost:3334/dock/update")))

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
  (do
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Raising volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "+5%")
      check :out slurp)
    (slurp "http://localhost:3334/dock/update")))

(defkbd volume-down
  [[] "XF86AudioLowerVolume"]
  (do
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Lowering volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "-5%")
      check :out slurp)
    (slurp "http://localhost:3334/dock/update")))

(defkbd spotify-volume-up
  [[:mod] "XF86AudioRaiseVolume"]
  (do
    (r.spotify/adjust-spotify-volume "up")
    (slurp "http://localhost:3334/dock/update")))

(defkbd spotify-volume-down
  [[:mod] "XF86AudioLowerVolume"]
  (do
    (r.spotify/adjust-spotify-volume "down")
    (slurp "http://localhost:3334/dock/update")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open Workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd open-workspace
  [[:mod] "o"]
  (do
    (notify/notify "Opening Workspace!")
    (workspaces/open-workspace)
    (slurp "http://localhost:3334/dock/update")))


(defkbd create-new-workspace
  [[:mod :shift] "o"]
  ;; TODO add support for creating a new one
  (notify/notify "Creating new Workspace!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE not completely working
(defkbd godot-reload-scene
  [[:mod] "i"]
  (let [godot-client                    (awm/client-for-class "Godot")
        {:awesome.client/keys [window]} godot-client]
    (notify/notify "sending keys to godot")
    (awm/lua-over-client window
                         (str "
local old_c = client.focus;
client.focus = c;

root.fake_input('key_press'  , 'Mod1');
root.fake_input('key_press'  , 'r');
root.fake_input('key_release', 'r');
root.fake_input('key_release', 'Mod1');

client.focus = old_c;
"))))

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
