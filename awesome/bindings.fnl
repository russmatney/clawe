(local gears (require "gears"))
(local awful (require "awful"))

(local fun (require "fun"))
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
     (key [:mod :shift] "r" restart-helper.save_state_and_restart)])

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
                 (awful.client.incwfact -0.05))))))

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
      (btn [:mod] 2 (fn [_c]
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
                     ;; (btn [] 4 awful.tag.viewnext)
                     ;; (btn [] 5 awful.tag.viewprev)
                     ))))

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
