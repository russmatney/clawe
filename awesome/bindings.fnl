(local gears (require "gears"))
(local awful (require "awful"))
(local naughty (require "naughty"))

(local fun (require "fun"))
(local tablex (require :pl.tablex))
(local clawe (require "clawe"))
(require "clawe-bindings")

;; (local dashboard (require :dashboard.dashboard))
(local helpers (require :dashboard.helpers))
(local restart-helper (require "./restart"))

(local exp {})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(local modifiers {:mod "Mod4"
                  :shift "Shift"
                  :ctrl "Control"})

(fn map-mods
  [mods]
  (->> mods
       (fun.map (partial . modifiers))
       (fun.totable)))

(fn key
  [mods key-code fun opt]
  (let [opt (or opt {})]
    (awful.key (map-mods mods) key-code fun opt)))

(set exp.key key)

(fn btn
  [mods btn-code fun]
  (awful.button (map-mods mods) btn-code fun))

(set exp.btn btn)

(local spawn-fn-cache {:spawn-fn-cache "me"})

(fn spawn-fn
  [cmd]
  "Prevents re-firing of the same command until the previous has completed.

Returns a function expected to be attached to a keybinding.
"
  (fn []
    (if (. spawn-fn-cache cmd)
        (do
          (pp "dropping call, fn not yet complete")
          (pp spawn-fn-cache)
          (naughty.notify {:title "Dropping binding call"
                           :text cmd})
          (gears.timer
           {:timeout 5
            :callback (fn []
                        (pp "Callback took longer than 5s, clearing.")
                        (tset spawn-fn-cache cmd nil)
                        (pp spawn-fn-cache))}) )
        (do
          (tset spawn-fn-cache cmd true)
          (awful.spawn.easy_async
           cmd
           (fn [stdout stderr _exitreason _exitcode]
             (tset spawn-fn-cache cmd nil)
             (when (and stdout (> (# stdout) 0)) (print stdout))
             (when (and stdout (> (# stdout) 0)) (print stderr))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global keybindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn centerwork_layout? []
  (-> (awful.screen.focused)
      (. :selected_tag)
      (. :layout)
      (. :name)
      ((fn [n]
         (or
          (= n "centerworkh")
          (= n "centerwork"))))))

(set
 _G.set_global_keys
 (fn []
   (if _G.append_clawe_bindings
       (_G.append_clawe_bindings))

   (awful.keyboard.append_global_keybindings
    [ ;; helpers
     (key [:mod :shift] "r" restart-helper.save_state_and_restart)

     ;; walk tags
     (key [:mod] "Left" awful.tag.viewprev)
     (key [:mod] "Right" awful.tag.viewnext)

     ;; previous tag
     (key [:mod] "Escape" awful.tag.history.restore)

     ;; TODO move all these bindings into ralphie itself
     ;; ralphie rofi
     (key [:mod] "x" (spawn-fn "ralphie rofi"))
     (key [:mod] "i" (spawn-fn "ralphie-emacs-cli '(org-clock-menu)'"))

     ;; scratchpads
     ;; TODO should pull the letter from workspaces.org
     ;; and write into ralphie-build-and-install
     (key [:mod] "z" (spawn-fn "clawe toggle-scratchpad zoom"))
     (key [:mod] "u" (spawn-fn "clawe toggle-scratchpad journal"))
     (key [:mod] "y" (spawn-fn "clawe toggle-scratchpad yodo"))
     (key [:mod] "g" (spawn-fn "clawe toggle-scratchpad garden"))
     (key [:mod] "t" (spawn-fn "clawe toggle-scratchpad web"))
     ;; (key [:mod] "b" (spawn-fn "clawe toggle-scratchpad chrome"))
     (key [:mod] "a" (spawn-fn "clawe toggle-scratchpad slack"))
     (key [:mod] "s" (spawn-fn "clawe toggle-scratchpad spotify"))
     (key [:mod] "e" (spawn-fn "clawe toggle-scratchpad chess"))

     ;; clawe keybindings
     (key [:mod] "r"
          (fn []
            ;; WARN potential race case on widgets reloading
            (awful.spawn "clawe rebuild-clawe" false)
            (awful.spawn "clawe reload" false)))

     (key [:mod] "d" (spawn-fn "clawe clean-workspaces"))
     (key [:mod] "o" (spawn-fn "clawe open-workspace"))
     (key [:mod] "w" (spawn-fn "clawe dwim"))

     ;; cycle layouts
     (key [:mod] "Tab"
          (fn []
            (let [scr (awful.screen.focused)]
              (awful.layout.inc 1 scr _G.layouts))))
     (key [:mod :shift] "Tab"
          (fn []
            (let [scr (awful.screen.focused)]
              (awful.layout.inc -1 scr _G.layouts))))

     ;; cycle workspaces
     (key [:mod] "n"
          (fn []
            (let [scr (awful.screen.focused)
                  current-tag scr.selected_tag
                  idx (if current-tag current-tag.index 1)
                  tag-count (tablex.size scr.tags)
                  next-idx (- idx 1)
                  next-idx (if (< next-idx 1)
                               tag-count
                               next-idx)
                  next-tag (. scr.tags next-idx)]
              (next-tag:view_only)
              (clawe.update-workspaces))))
     (key [:mod] "p"
          (fn []
            (let [scr (awful.screen.focused)
                  current-tag scr.selected_tag
                  idx (if current-tag current-tag.index 1)
                  tag-count (tablex.size scr.tags)
                  next-idx (+ idx 1)
                  next-idx (if (> next-idx tag-count)
                               1
                               next-idx)
                  next-tag (. scr.tags next-idx)]
              (next-tag:view_only)
              (clawe.update-workspaces))))

     (key [:mod :shift] "n" (spawn-fn "clawe drag-workspace-index down"))
     (key [:mod :shift] "p" (spawn-fn "clawe drag-workspace-index up"))

     ;; ;; terminal
     ;; (key [:mod] "Return"
     ;;      (fn []
     ;;        (let [current-tag (. (awful.screen.focused) :selected_tag)
     ;;              name current-tag.name
     ;;              str (.. "ralphie-open-term " name)]
     ;;          (awful.spawn str))))

     ;; ;; emacs
     ;; (key [:mod :shift] "Return" (spawn-fn "ralphie-open-emacs"))

     ;; launcher (rofi)
     (key [:mod] "space" (spawn-fn "/usr/bin/rofi -show combi"))

     ;; screenshots
     (key [:mod :shift] "s" (spawn-fn "ralphie screenshot full"))
     (key [:mod :shift] "a" (spawn-fn "ralphie screenshot region"))

     ;; brightness
     (key [] "XF86MonBrightnessUp" (spawn-fn "light -A 5"))
     (key [] "XF86MonBrightnessDown" (spawn-fn "light -U 5"))

     ;; media controls
     ;; TODO play-pause should create spotify if its not open
     (key [] "XF86AudioPlay" (spawn-fn "spotifycli --playpause"))
     (key [] "XF86AudioNext" (spawn-fn "playerctl next"))
     (key [] "XF86AudioPrev" (spawn-fn "playerctl previous"))
     (key [] "XF86AudioMute" (spawn-fn "pactl set-sink-mute @DEFAULT_SINK@ toggle"))
     (key [] "XF86AudioRaiseVolume" (spawn-fn "pactl set-sink-volume @DEFAULT_SINK@ +5%"))
     (key [] "XF86AudioLowerVolume" (spawn-fn "pactl set-sink-volume @DEFAULT_SINK@ -5%"))
     (key [:mod] "XF86AudioRaiseVolume" (spawn-fn "ralphie spotify-volume up"))
     (key [:mod] "XF86AudioLowerVolume" (spawn-fn "ralphie spotify-volume down"))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; numbered Tag global keybindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   (for [it 0 10]
     (awful.keyboard.append_global_keybindings
      [ ;; show tag (workspace)
       (key [:mod] (.. "#" (+ 9 it))
            (fn []
              (let [scr (awful.screen.focused)
                    keyed-tag (. scr.tags it)]
                (if keyed-tag
                    (do
                      (helpers.tag_back_and_forth keyed-tag.index)
                      (clawe.update-workspaces))
                    (let []
                      ;; create tag
                      ;; TODO fetch name  from config for index
                      ;; include other tag config?
                      (awful.tag.add (.. "num" it) {:layout (. layouts 1)
                                                    :selected true}))))))

       ;; add tag to current perspective
       (key [:mod :ctrl] (.. "#" (+ 9 it))
            (fn []
              (let [scr (awful.screen.focused)
                    scr-tag (. scr.tags it)]
                (when scr-tag (awful.tag.viewtoggle scr-tag)))
              (clawe.update-workspaces)))

       ;; move current focus to tag (workspace)
       (key [:mod :shift] (.. "#" (+ 9 it))
            (fn []
              (when _G.client.focus
                (let [scr-tag (. _G.client.focus.screen.tags it)]
                  (when scr-tag
                    (_G.client.focus:move_to_tag scr-tag)
                    (clawe.update-workspaces))))))

       ;; add/remove focused client on tag
       (key [:mod :shift :ctrl] (.. "#" (+ 9 it))
            (fn []
              (when _G.client.focus
                (let [scr-tag (. _G.client.focus.screen.tags it)]
                  (when scr-tag
                    (_G.client.focus:toggle_tag scr-tag)
                    (clawe.update-workspaces))))))]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client Keybindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn focus-move [dir centerwork-dir centerwork-dir2]
  ;; TODO should move floating windows if floating
  ;; TODO consider toggling when there is a floating window
  (if (centerwork_layout?)
      (do
        (awful.client.focus.bydirection centerwork-dir)
        (awful.client.focus.bydirection centerwork-dir2)
        (when _G.client.focus
          (_G.client.focus:swap (awful.client.getmaster))))

      (awful.client.focus.bydirection dir)))

(fn move-client [c dir]
  (if
   (= "right" dir) (set c.x (+ c.x 10))
   (=  "left" dir) (set c.x (+ c.x -10))
   ;; y 0 at top
   (= "up" dir) (set c.y (+ c.y -10))
   (= "down" dir) (set c.y (+ c.y 10)))
  )

;; exported to add to global rules
(set exp.clientkeys
     (gears.table.join
      ;; kill current client
      (key [:mod] "q" (fn [c] (c:kill)))

      ;; toggle floating
      (key [:mod] "f" awful.client.floating.toggle)

      ;; focus movement
      (key [:mod :shift] "l" (fn [c]
                               (if c.floating
                                   (move-client c "right")
                                   (focus-move "right" "right" "up"))))
      (key [:mod :shift] "h" (fn [c]
                               (if c.floating
                                   (move-client c "left")
                                   (focus-move "left" "left" "down"))))
      (key [:mod :shift] "j" (fn [c]
                               (if c.floating
                                   (move-client c "down")
                                   (focus-move "down" "right" "down"))))
      (key [:mod :shift] "k" (fn [c]
                               (if c.floating
                                   (move-client c "up")
                                   (focus-move "up" "left" "up"))))

      ;; widen/shink windows
      (key [:ctrl :shift] "l"
           (fn [c]
             (if c.floating
                 (do
                   (awful.placement.scale
                    c {:direction "right"
                       :by_percent 1.1})
                   (awful.placement.scale
                    c {:direction "left"
                       :by_percent 1.1})
                   )
                 (awful.tag.incmwfact 0.05))))
      (key [:ctrl :shift] "h"
           (fn [c]
             (if c.floating
                 (do
                   (awful.placement.scale
                    c {:direction "left"
                       :by_percent 0.9})
                   (awful.placement.scale
                    c {:direction "right"
                       :by_percent 0.9})
                   )
                 (awful.tag.incmwfact -0.05))))
      (key [:ctrl :shift] "j"
           (fn [c]
             (if c.floating
                 (do
                   (awful.placement.scale
                    c {:direction "down"
                       :by_percent 1.1})
                   (awful.placement.scale
                    c {:direction "up"
                       :by_percent 1.1})
                   )
                 (awful.client.incwfact 0.05))))
      (key [:ctrl :shift] "k"
           (fn [c]
             (if c.floating
                 (do
                   (awful.placement.scale
                    c {:direction "up"
                       :by_percent 0.9})
                   (awful.placement.scale
                    c {:direction "down"
                       :by_percent 0.9})
                   )
                 (awful.client.incwfact -0.05))))

      ;; center on screen
      (key [:mod] "c"
           (fn [c]
             (-> c
                 (tset :floating true)
                 ((+ awful.placement.scale
                     awful.placement.centered)
                  {:honor_padding true
                   :honor_workarea true
                   :to_percent 0.75}))))

      ;; large centered
      (key [:mod :shift] "c"
           (fn [c]
             (-> c
                 (tset :floating true)
                 ((+ awful.placement.scale
                     awful.placement.centered)
                  {:honor_padding true
                   :honor_workarea true
                   :to_percent 0.9}))))

      ;; center without resizing
      (key [:mod :ctrl] "c"
           (fn [c]
             (-> c
                 (tset :floating true)
                 (awful.placement.centered
                  {:honor_padding true
                   :honor_workarea true}))))

      ;; swap with master
      (key [:mod :ctrl] "Return" (fn [c] (c:swap (awful.client.getmaster))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mouse bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; exported to add to global rules
(set exp.clientbuttons
     (gears.table.join
      (btn [] 1 (fn [c]
                  (tset _G.client :focus c)
                  (c:raise)))
      (btn [:mod] 1 awful.mouse.client.move)
      (btn [:mod] 2 (fn [c]
                      ;; (naughty.notify
                      ;;  {:title (.. "Pulling "
                      ;;              (or c.name "client")
                      ;;              " above and ontop")})
                      ;; (tset _G.client :focus c)
                      ;; not sure why this doesn't adjust the client props
                      ;; (tset _G.client.focus :ontop true)
                      ;; (tset _G.client.focus :above true)
                      ))
      (btn [:mod] 3 awful.mouse.client.resize)
      ;; (btn [:mod] 4 (fn [c]
      ;;                 (ppi c)
      ;;                 (naughty.notify {:title "Btn 4 clicked"
      ;;                                  :text c.name})))
      ;; (btn [:mod] 5 (fn [c]
      ;;                 (ppi c)
      ;;                 (naughty.notify {:title "Btn 5 clicked"
      ;;                                  :text c.name})))
      ))

(global
 init_root_buttons
 (fn []
   (_G.root.buttons (gears.table.join
                     ;; (btn [] 1 mymainmenu:hide)
                     ;; (btn [] 3 mymainmenu:toggle)
                     (btn [] 4 awful.tag.viewnext)
                     (btn [] 5 awful.tag.viewprev)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Titlebar buttons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set exp.titlebarbuttons
     (fn [c]
       (gears.table.join
        (btn [] 1 (fn []
                    (set client.focus c)
                    (c:raise)
                    (awful.mouse.client.move c)))
        (btn [] 3 (fn []
                    (set client.focus c)
                    (c:raise)
                    (awful.mouse.client.resize c))))))

exp
