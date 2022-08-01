(ns clawe.defs.bindings
  (:require
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [defthing.defcom :as defcom]
   [defthing.defkbd :refer [defkbd]]

   [ralphie.notify :as notify]
   [ralphie.clipboard :as r.clip]
   [ralphie.screenshot :as r.screenshot]
   [ralphie.awesome :as awm]
   [ralphie.spotify :as r.spotify]
   [ralphie.pulseaudio :as r.pulseaudio]

   [clawe.client :as client]
   [clawe.defs.workspaces :as defs.workspaces]
   [clawe.doctor :as clawe.doctor]
   [clawe.m-x :as c.m-x]
   [clawe.scratchpad :as scratchpad]
   [clawe.toggle :as toggle]
   [clawe.workspaces :as workspaces]
   [clawe.rules :as c.rules]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi, launchers, command selectors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO should these live in m-x?
(defkbd clawe-rofi-sxhkd
  [[:mod] "w"]
  (defcom/exec c.m-x/m-x))

(defkbd clawe-rofi-awm
  [[:mod] "x"]
  {:binding/awm true}
  ;; {:binding/awm true}
  (defcom/exec c.m-x/m-x))

(defkbd rofi-launcher
  [[:mod] "space"]
  (->
    ^{:out :string}
    ($ rofi -show combi -combi-modi "window,drun")
    check :out))

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
      (client/focus-client {:center?   true
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
    (client/focus-client {:center?   true
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
      ;; TODO consider merging db workspaces here
      ;; not sure what we'd need to read
      ;; but could be a dynamic scratchpad config
      scratchpad/toggle-scratchpad)
  (clawe.doctor/update-topbar))

(comment
  (workspaces/merge-awm-tags defs.workspaces/dev-browser))

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

(defkbd toggle-workspace-discord
  [[:mod :shift] "d"]
  (toggle-workspace defs.workspaces/discord))

(defkbd toggle-workspace-spotify
  [[:mod] "s"]
  (toggle-workspace defs.workspaces/spotify))

;; (defkbd toggle-workspace-godot
;;   [[:mod] "g"]
;;   (toggle-workspace defs.workspaces/godot))

(defkbd toggle-workspace-zoom
  [[:mod] "z"]
  (toggle-workspace defs.workspaces/zoom))

(defkbd toggle-workspace-one-password
  [[:mod] "."]
  (fn [_ _] (toggle-workspace defs.workspaces/one-password)))

;; (defkbd toggle-workspace-pixels
;;   [[:mod :shift] "p"]
;;   (fn [_ _] (toggle-workspace defs.workspaces/pixels)))

(defkbd toggle-workspace-doctor-popup
  [[:mod :shift] "p"]
  (fn [_ _] (toggle-workspace defs.workspaces/doctor-popup)))

(defkbd toggle-workspace-doctor-todo
  [[:mod] "y"]
  (fn [_ _] (toggle-workspace defs.workspaces/doctor-todo)))


(defkbd toggle-terminal
  [[:mod] "Return"]
  (toggle/toggle-app {:client-name "terminal" :app-name "Alacritty"}))


(defkbd toggle-emacs
  [[:mod :shift] "Return"]
  (toggle/toggle-app {:client-name "emacs" :app-name "Emacs"}))

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
  (awm/awm-fnl '(awful.client.focus.byidx 1))
  ;; (do
  ;;   ;; TODO should be able to notify from sxhkd, but run this awm-fnl from awesome
  ;;   (notify/notify "Client focus next!" (seq (awm/visible-clients))))
  )

(defkbd cycle-focus-prev
  [[:mod :shift] "Tab"]
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
    (clawe.doctor/update-topbar))
  )

(defkbd drag-workspace-next
  [[:mod :shift] "Right"]
  (do
    (workspaces/drag-workspace "up")
    (clawe.doctor/update-topbar))
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
    '(awful.spawn.easy_async "light -A 5" (fn []))))

(defkbd brightness-down
  [[] "XF86MonBrightnessDown"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "light -U 5" (fn []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Play/Pause/Next/Prev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO support in sxhkd
(defkbd spotify-pause
  [[] "XF86AudioPause"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "spotifycli --playpause" (fn []))))

(defkbd spotify-play
  [[] "XF86AudioPlay"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "spotifycli --playpause" (fn []))))

(defkbd audio-next
  [[] "XF86AudioNext"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "playerctl next" (fn []))))

(defkbd audio-prev
  [[] "XF86AudioPrev"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "playerctl previous" (fn []))))

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
    (clawe.doctor/update-topbar)))

(defkbd toggle-output-mute
  [[] "XF86AudioMute"]
  (awm/awm-fnl
    '(awful.spawn.easy_async "pactl set-sink-mute @DEFAULT_SINK@ toggle" (fn []))))

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
;; Open Workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd open-workspace
  [[:mod] "o"]
  (do
    (notify/notify "Opening Workspace!")
    (workspaces/open-workspace)
    (clawe.doctor/update-topbar)))


(defkbd create-new-workspace
  [[:mod :shift] "o"]
  ;; TODO add support for creating a new one
  (notify/notify "Creating new Workspace!"))

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
