(ns clawe.defs.bindings
  (:require
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [defthing.defkbd :refer [defkbd]]

   [ralphie.notify :as notify]
   [ralphie.clipboard :as r.clip]
   [ralphie.screenshot :as r.screenshot]
   [ralphie.awesome :as awm]
   [ralphie.spotify :as r.spotify]
   [ralphie.pulseaudio :as r.pulseaudio]

   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.sxhkd.bindings :refer [sxhkd-exec]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi, launchers, command selectors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd clawe-rofi-mx
  [[:mod] "x"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.m-x/m-x"))

(defkbd rofi-launcher
  [[:mod] "space"]
  (sxhkd-exec "rofi -show combi -combi-modi 'window,drun'"))

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
    (clawe.doctor/update-screenshots)))

(defkbd screenshot-region
  [[:mod :shift] "a"]
  (do
    (r.screenshot/select-region)
    (clawe.doctor/update-screenshots)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd uuid-on-clipboard
  [[:mod :ctrl] "u"]
  (let [uuid (str (java.util.UUID/randomUUID))]
    (notify/notify "clippy!")
    (r.clip/set-clip uuid)))

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
       (tset c :ontop c.floating)
       (tset c :above c.floating)
       (awful.client.floating.toggle c)
       (if c.floating
         (c:raise)))))

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

  ;; note that this is less performant than before
  ;; - it will now fire via sxhkd and use three awm/awm-fnl calls,
  ;; rather than being caught by awesome itself
  (do
    (let [current-window (awm/awm-fnl '(view _G.client.focus.window))
          current-client {:awesome.client/window current-window}]
      ;; using this to take advantage of focus-client's :bury-all? option
      ;; that function could use a refactor
      ;; - maybe it's just an update-client-state function
      (awm/focus-client {:center?   true
                         :float?    true
                         :bury-all? true} current-client))
    (awm/awm-fnl
      '(let [c _G.client.focus]
         (tset c :ontop true)
         (tset c :floating true)
         (-> c
             ((+ awful.placement.scale
                 awful.placement.centered)
              {:honor_padding  true
               :honor_workarea true
               :to_percent     0.75}))))))

(comment
  (let [current-window (awm/awm-fnl '(view _G.client.focus.window))
        current-client {:awesome.client/window current-window}]
    (awm/focus-client {:center?   true
                       :float?    true
                       :bury-all? true} current-client)))

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
;; Notifications
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd toggle-notifications-center
  [[:mod :alt] "n"]
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

(defkbd toggle-terminal
  [[:mod] "Return"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key terminal"))

(defkbd toggle-emacs
  [[:mod :shift] "Return"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key emacs"))

(defkbd toggle-workspace-journal
  [[:mod] "u"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key journal"))

(defkbd toggle-workspace-web
  [[:mod] "t"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key web"))

(defkbd toggle-workspace-chrome-browser
  [[:mod] "b"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key devweb"))

(defkbd toggle-workspace-slack
  [[:mod] "a"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key slack"))

(defkbd toggle-workspace-discord
  [[:mod :shift] "d"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key discord"))

(defkbd toggle-workspace-spotify
  [[:mod] "s"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key spotify"))

(defkbd toggle-workspace-zoom
  [[:mod] "z"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key zoom"))

(defkbd toggle-workspace-one-password
  [[:mod] "."]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key onepass"))

(defkbd toggle-workspace-doctor-todo
  [[:mod] "y"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key doctor"))

(defkbd toggle-workspace-aseprite
  [[:mod :shift] "p"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key aseprite"))

(defkbd toggle-workspace-godot
  [[:mod :shift] "g"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle --key godot"))

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
  [[:mod] "p"]
  (awm/awm-fnl
    '(do (awful.tag.viewnext)
         (awful.spawn.easy_async "curl http://localhost:3334/topbar/update" (fn [])))))

(defkbd cycle-prev-tag-2
  [[:mod] "n"]
  (awm/awm-fnl
    '(do (awful.tag.viewprev)
         (awful.spawn.easy_async "curl http://localhost:3334/topbar/update" (fn [])))))

(defkbd cycle-focus-next
  [[:mod] "Tab"]
  (awm/awm-fnl '(awful.client.focus.byidx 1)))

(defkbd cycle-focus-prev
  [[:mod :shift] "Tab"]
  (awm/awm-fnl '(awful.client.focus.byidx -1)))

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
    (wm/drag-workspace :dir/down)
    (clawe.doctor/update-topbar)))

(defkbd drag-workspace-next
  [[:mod :shift] "Right"]
  (do
    (wm/drag-workspace :dir/up)
    (clawe.doctor/update-topbar)))

(defkbd correct-clients-and-workspaces
  [[:mod] "d"]
  (sxhkd-exec "bb --config ~/russmatney/clawe/bb.edn -x clawe.rules/clean-up-workspaces"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Brightness
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd brightness-up
  [[] "XF86MonBrightnessUp"]
  (sxhkd-exec "light -A 5"))

(defkbd brightness-down
  [[] "XF86MonBrightnessDown"]
  (sxhkd-exec "light -U 5"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Play/Pause/Next/Prev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd spotify-pause
  [[] "XF86AudioPause"]
  (sxhkd-exec "spotifycli --playpause"))

(defkbd spotify-play
  [[] "XF86AudioPlay"]
  (sxhkd-exec "spotifycli --playpause"))

(defkbd audio-next
  [[] "XF86AudioNext"]
  (sxhkd-exec "playerctl next"))

(defkbd audio-prev
  [[] "XF86AudioPrev"]
  (sxhkd-exec "playerctl previous"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muting input/output
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd toggle-input-mute
  [[:mod] "m"]
  (do
    (->
      ($ amixer set Capture toggle)
      check :out slurp)
    (notify/notify
      {:notify/subject "Mute Toggled!"
       :notify/body    (if (r.pulseaudio/input-muted?)
                         "Muted!" "Unmuted!")
       :notify/id      "mute-notif"})
    (clawe.doctor/update-topbar)))

(defkbd toggle-output-mute
  [[] "XF86AudioMute"]
  (sxhkd-exec "pactl set-sink-mute @DEFAULT_SINK@ toggle"))

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
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "+5%")
      check :out slurp)
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Raising volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (clawe.doctor/update-topbar)))

(defkbd volume-down
  [[] "XF86AudioLowerVolume"]
  (do
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "-5%")
      check :out slurp)
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Lowering volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (clawe.doctor/update-topbar)))

(defkbd spotify-volume-up
  [[:mod] "XF86AudioRaiseVolume"]
  (do
    (r.spotify/adjust-spotify-volume "up")
    (clawe.doctor/update-topbar)))

(defkbd spotify-volume-down
  [[:mod] "XF86AudioLowerVolume"]
  (do
    (r.spotify/adjust-spotify-volume "down")
    (clawe.doctor/update-topbar)))

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
