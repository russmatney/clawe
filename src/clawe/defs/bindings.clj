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
   [clawe.m-x :as c.m-x]
   [clawe.scratchpad :as scratchpad]
   [clawe.workspaces :as workspaces]
   [clawe.rules :as c.rules]
   [ralphie.yabai :as yabai]))

(defn update-topbar []
  (slurp "http://localhost:3334/topbar/update"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi, launchers, command selectors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
      (c.awm/focus-client {:center?   true
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
    (c.awm/focus-client {:center?   true
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
  (update-topbar))

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
  [{:keys [wsp->client wsp->open-client]}]
  (let [wsp    (workspaces/current-workspace)
        client (wsp->client wsp)
        client-focused
        (or
          (:yabai.window/has-focus client)
          (:awesome.client/focused client))]
    (cond
      ;; no tag
      (not wsp)
      ;; TODO probably better to create the 'right' tag here if possible
      ;; maybe prompt for a tag name?
      (do
        (notify/notify "No current tag found...?" "Creating fallback")
        (awm/create-tag! "temp-tag")
        (awm/focus-tag! "temp-tag")
        (wsp->open-client nil))

      ;; client focused
      (and client client-focused)
      (cond
        notify/is-mac?
        (yabai/close-window client)

        :else
        (awm/close-client client))

      ;; client not focused
      (and client (not client-focused))
      (cond
        notify/is-mac?
        ;; TODO center and float? and bury?
        (yabai/focus-window client)

        :else
        ;; DEPRECATED
        (c.awm/focus-client {:center?   false
                             :float?    false
                             :bury-all? false} client))

      ;; tag but no client
      (not client)
      (do
        (wsp->open-client wsp)
        nil))))

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
    (update-topbar))
  )

(defkbd drag-workspace-next
  [[:mod :shift] "Right"]
  (do
    (workspaces/drag-workspace "up")
    (update-topbar))
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
;; (defkbd spotify-pause
;;   [[] "XF86AudioPause"]
;;   (r.spotify/spotifycli "--playpause"))

;; (defkbd spotify-play
;;   [[] "XF86AudioPlay"]
;;   (r.spotify/spotifycli "--playpause"))

;; DEPRECATED support in sxhkd
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
    (update-topbar)))

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
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Raising volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "+5%")
      check :out slurp)
    (update-topbar)))

(defkbd volume-down
  [[] "XF86AudioLowerVolume"]
  (do
    (notify/notify {:notify/id      "volume"
                    :notify/subject "Lowering volume"
                    :notify/body    (r.pulseaudio/default-sink-volume-label)})
    (->
      ($ pactl set-sink-volume "@DEFAULT_SINK@" "-5%")
      check :out slurp)
    (update-topbar)))

(defkbd spotify-volume-up
  [[:mod] "XF86AudioRaiseVolume"]
  (do
    (r.spotify/adjust-spotify-volume "up")
    (update-topbar)))

(defkbd spotify-volume-down
  [[:mod] "XF86AudioLowerVolume"]
  (do
    (r.spotify/adjust-spotify-volume "down")
    (update-topbar)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open Workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defkbd open-workspace
  [[:mod] "o"]
  (do
    (notify/notify "Opening Workspace!")
    (workspaces/open-workspace)
    (update-topbar)))


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
