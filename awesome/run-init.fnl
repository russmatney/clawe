;; named `run-init` rather than `init` to prevent accidental lua module loading

(local awful (require "awful"))
(local gears (require "gears"))
(local naughty (require "naughty"))
(local beautiful (require "beautiful"))
(local view (require :fennelview))
(local inspect (require :inspect))
(local lain (require "lain"))
(local util (require "util"))

;; focus client after awesome.restart
(require "awful.autofocus")
(require "steamfix")


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; Global Helpers
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(global pp (fn [x] (print (view x))))
(global ppi (fn [x] (print (inspect x))))

(global layouts
        [awful.layout.suit.tile
         ;; awful.layout.suit.floating
         ;; awful.layout.suit.fair
         ;; awful.layout.suit.magnifier
         ;; awful.layout.suit.spiral
         ;; awful.layout.suit.spiral.dwindle
         lain.layout.centerwork
         ;; lain.layout.centerwork.horizontal
         ])

(global update-topbar
        (fn [] (awful.spawn.easy_async "curl http://localhost:3334/topbar/update" (fn []))))

(global reload-doctor
        (fn reload-doctor [] (awful.spawn.easy_async "curl http://localhost:3334/reload" (fn []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Theming
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn init_theme []
  ;; init theme
  (-> (require "gears.filesystem")
      (. :get_configuration_dir)
      ((fn [f] (f)))
      (.. "theme/theme.lua")
      beautiful.init)
  (set beautiful.icon_theme "Papirus-Dark")
  (set beautiful.bg_normal "#141A1B")
  (set beautiful.bg_focus "#222B2E")
  (set beautiful.font "Noto Sans Regular 12")
  (set beautiful.notification_font "Noto Sans Bold 14")
  (set beautiful.notification_max_height 100)
  (if (util.is_vader)
      (set beautiful.useless_gap 6)
      (set beautiful.useless_gap 12)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; External Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set
 _G.reapply_rules
 (fn []
   (each [c (awful.client.iterate (fn [_] true))]
     (awful.rules.apply c))))

(set
 _G.set_layout
 (fn [layout]
   (awful.layout.set layout)))

(set
 _G.set_geometry
 (fn [window-id geo]
   ;; should only call once, presuming unique window-ids
   (each [c (awful.client.iterate (fn [c] (= c.window window-id)))]
     (pp {:event "Setting client geometry"
          :window-id window-id
          :client c
          :geometry geo})
     (c:geometry geo))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garbage collection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set
 _G.log_garbage
 (fn []
   (pp {:log :garbage-counts
        :count (collectgarbage "count")
        ;; :button (button.instances)
        ;; :client (client.instances)
        ;; :drawable (drawable.instances)
        ;; :drawin (drawin.instances)
        ;; :key (key.instances)
        ;; :screen (screen.instances)
        ;; :tag (tag.instances)
        })))

(set
 _G.handle_garbage
 (fn []
   (local before (collectgarbage "count"))
   (collectgarbage)
   (local after (collectgarbage "count"))
   ;; (print (.. "before: " before " after: " after))
   (local count-diff (- before after))
   ;; (naughty.notify {:title "Collected Garbage"
   ;;                  :text (.. "Count: " count-diff)})

   ;; call again until the diff is < 100
   (if (> count-diff 100)
       (_G.handle_garbage)
       (do
         (_G.log_garbage)
         (naughty.notify {:title "Remaining Garbage"
                          :text (.. "Count: " after)})))))

;; garbage timer
(gears.timer
 {:timeout 300
  :autostart true
  :call_now true
  :callback _G.handle_garbage})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; init deps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(local restart-helper (require "./restart"))
(require :errors)
(require :bar)
(require :remote)
(local rules (require :rules))
(local signals (require :signals))
(local titlebars (require :titlebars))
(local clawe (require :clawe))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn to-client-data [c]
  {:name     c.name
   :ontop    c.ontop
   :window   c.window
   :type     c.type
   :class    c.class
   :instance c.instance
   :pid      c.pid
   :role     c.role})

(global
 init
 (fn [config]
   ;; error handling
   (print "init_error_handling")
   (_G.init_error_handling config)

   ;; init remote
   (print "skipping init_remote...")
   ;; (_G.init_remote config)

   ;; theme
   (print "init_theme")
   (init_theme config)

   ;; bindings
   (print "init bindings")
   (_G.set_global_keys config)
   (_G.init_root_buttons config)

   ;; bar and widgets
   (print "init bars and widgets")
   (_G.init_bar config)

   ;; signals
   (print "init_signals")
   (signals.init_manage_signal config)
   (signals.init_focus_signals config)
   (titlebars.init_request_titlebars config)

   ;; (client.connect_signal
   ;;  "property::name"
   ;;  (fn [c]
   ;;    (pp {:signal "property::name" :c (to-client-data c)})
   ;;    (clawe.cmd-args "apply-rules-to-client" (to-client-data c))))
   ;; (client.connect_signal
   ;;  "manage"
   ;;  (fn [c]
   ;;    (pp {:signal "manage" :c (to-client-data c)})
   ;;    (clawe.cmd-args "apply-rules-to-client" (to-client-data c))))
   ;; (client.connect_signal
   ;;  "request::urgent"
   ;;  (fn [c]
   ;;    (pp {:signal "request::urgent"})
   ;;    (clawe.cmd-args "apply-rules-to-client" (to-client-data c))))
   ;; (client.connect_signal "focus" (fn [_] (update-topbar)))
   ;; (tag.connect_signal "property::screen" (fn [_] (update-topbar)))
   ;; (tag.connect_signal "tagged" (fn [_] (update-topbar)))
   ;; (tag.connect_signal "untagged" (fn [_] (update-topbar)))
   ;; (tag.connect_signal "property::urgent" (fn [_] (update-topbar)))

   (print "init_rules")
   (rules.init_rules config)

   (print "init_tags/restore_state")
   (restart-helper.restore_state)

   ;; spawns
   (print "init_spawns")
   (awful.spawn "~/.config/awesome/autorun.sh" false)
   (awful.spawn "xset r rate 170 60" false)

   ;; reapply rules after restoring state of clients/tags
   (print "reapplying rules")
   (_G.reapply_rules)
   (clawe.cmd "clawe-apply-rules")
   (reload-doctor)

   (print "------------------Awesome Init Complete---------")))

(_G.init)
