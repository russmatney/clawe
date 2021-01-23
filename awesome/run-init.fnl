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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  (set beautiful.font "Noto Sans Regular 10")
  (set beautiful.notification_font "Noto Sans Bold 14")
  (set beautiful.notification_max_height 100)
  (if (util.is_vader)
      (set beautiful.useless_gap 10)
      (set beautiful.useless_gap 20)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; External Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(global
 reapply_rules
 (fn []
   (each [c (awful.client.iterate (fn [_] true))]
     (awful.rules.apply c))))

(global
 set_layout
 (fn [layout]
   (awful.layout.set layout)))

(global
 set_geometry
 (fn [window-id geo]
   ;; should only call once, presuming unique window-ids
   (each [c (awful.client.iterate (fn [c] (= c.window window-id)))]
     (pp {:event "Setting client geometry"
          :window-id window-id
          :client c
          :geometry geo})
     (c:geometry geo))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tags init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(local restart-helper (require "./restart"))
(require :errors)
(require :bar)
(require :remote)
(local rules (require :rules))
(local signals (require :signals))
(local titlebars (require :titlebars))
(local spawns (require :spawns))

(fn clear_urgent_clients []
  (each [_ cl (pairs (_G.client.get))]
    (set cl.urgent false)))

(global
 init_tags
 (fn [config]
   (when (and config (. config :tag_names))
     (let [tag-names (and config config.tag_names)]
       (each [_ tag-name (pairs tag-names)]
         (let [existing-tag (-> _G.mouse.screen.tags
                                (awful.tag.find_by_name tag-name))]
           (if existing-tag
               (print (..  "Tag " tag-name " exists"))
               (do
                 (print (..  "Creating tag " tag-name))
                 (awful.tag.add tag-name {:layout (. layouts 1)})))))))

   (restart-helper.restore_state)

   ;; reapply rules to all clients
   (_G.reapply_rules)

   (clear_urgent_clients)

   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garbage collection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(global
 handle_garbage
 (fn []
   (print "Collecting garbage")
   (local before (collectgarbage "count"))
   (collectgarbage)
   (local after (collectgarbage "count"))
   (print (.. "before: " before " after: " after))
   (local count-diff (- before after))
   (naughty.notify {:title "Collected Garbage"
                    :text (.. "Count: " count-diff)})


   (print "objects alive:")
   (each [name obj (pairs {: button : client
                           : drawable : drawin
                           : key : screen : tag})]
     (print name (obj.instances)))

   ;; call again until the diff is < 100
   (if (> count-diff 100)
       (_G.handle_garbage)
       (naughty.notify {:title "Remaining Garbage"
                        :text (.. "Count: " after)}))))

;; garbage timer
(gears.timer
 {:timeout 300
  :autostart true
  :callback _G.handle_garbage})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(global
 init
 (fn [config]
   ;; error handling
   (print "init_error_handling")
   (_G.init_error_handling config)

   ;; init remote
   (print "init_remote")
   (_G.init_remote config)

   ;; theme
   (print "init_theme")
   (init_theme config)

   ;; bindings
   (print "init bindings")
   (_G.set_global_keys config)
   (_G.init_root_buttons config)

   ;; calls into init_tags with built config
   (print "init ralphie-tags")

   ;; bar and widgets
   (print "init screen and tags")
   (_G.init_bar config)

   ;; signals
   (print "init_signals")
   (signals.init_manage_signal config)
   (signals.init_focus_signals config)
   (titlebars.init_request_titlebars config)

   (print "init_rules")
   (rules.init_rules config)

   ;; spawns
   (print "init_spawns")
   (spawns.init_spawns config)

   ;; TODO should this be an async spawn?
   (awful.spawn "ralphie init-tags")

   (print "------------------Awesome Init Complete---------")
   ))

(_G.init)
