;; named `run-init` rather than `init` to prevent accidental lua module loading

(local awful (require "awful"))
(local beautiful (require "beautiful"))
(local view (require :fennelview))
(local inspect (require :inspect))
(local lain (require "lain"))

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

(fn _G.init_theme []
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
  ;; TODO if hostname algo/vader, use 30/10
  ;; (set beautiful.useless_gap 30)
  (set beautiful.useless_gap 10))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; External (Ralphie?) Functions
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
(require :rules)
(require :signals)
(require :titlebars)
(require :spawns)

(fn clear_urgent_clients []
  (each [_ cl (pairs (_G.client.get))]
    (set cl.urgent false)))

(global
 init_tags
 (fn [config]
   (when (and config (. config :tag_names))
     (print "found tag_names in config")
     (pp config.tag_names)

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


(fn ralphie-init []
  (awful.spawn "ralphie init-tags"))

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
   (print before)
   (print after)
   ;; call again until the diff is < 100
   (if (> (- before after) 100)
       (_G.handle_garbage))))

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
   (_G.init_theme config)

   ;; bindings
   (print "init bindings")
   (_G.set_global_keys config)
   (_G.init_root_buttons config)

   ;; tags, then restore state, then apply rules to all clients...?
   ;; calls into init_tags with built config
   (ralphie-init)

   ;; bar and widgets
   (print "init screen and tags")
   (_G.init_bar config)

   ;; signals
   (print "init_signals")
   (_G.init_manage_signal config)
   (_G.init_request_titlebars config)
   (_G.init_focus_signals config)
   (_G.init_arrange_signal config)

   (_G.init_rules config)

   ;; spawns
   (print "init_spawns")
   (_G.init_spawns config)

   (print "------------------Awesome Init Complete---------")
   ))

;; (awful.spawn "ralphie awesome-init")
(_G.init)
