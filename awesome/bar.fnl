(local wibox (require "wibox"))
(local gears (require "gears"))
(local beautiful (require "beautiful"))

(local dirty-repos (require "widgets.dirty-repos"))
(local org-clock (require "widgets.org-clock"))
(local workspaces (require "widgets.workspaces"))
(local workrave (require "widgets.workrave"))

;; (local pomodoro-widget (require "awesome-wm-widgets.pomodoroarc-widget.pomodoroarc"))
(local batteryarc-widget (require"awesome-wm-widgets.batteryarc-widget.batteryarc"))
(local spotify-widget (require"awesome-wm-widgets.spotify-widget.spotify"))

(local awful (require "awful"))
(local util (require "util"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wallpaper handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; this is in here b/c it is called in awful.screen.connect_for_each_screen
(global
 set_wallpaper
 (fn [s]
   (pp "setting wallpaper")
   (let [s (or s (_G.mouse.screen))
         wp-path beautiful.wallpaper]
     (gears.wallpaper.maximized wp-path s true))))

;; Re-set wallpaper when a screen's geometry changes (e.g. different resolution)
(screen.connect_signal "property::geometry" (fn [s] (_G.set_wallpaper s)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WIBAR
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Create a textclock widget
(local mytextclock (wibox.widget.textclock "%H:%M "))

(local blue "#9EBABA")
(local separator (wibox.widget.textbox
                  (.. "<span color=\"" blue "\"> | </span>")))


(global
 init_bar
 (fn []
   (awful.screen.connect_for_each_screen
    (fn [s]
      (_G.set_wallpaper s)

      (util.log_if_error
       (fn []
         ;; remove if it exists already
         (if s.top-bar (s.top-bar:remove))
         (if s.bottom-bar (s.bottom-bar:remove))

         ;; Create the wibox
         (set s.top-bar
              (awful.wibar
               {:position "top"
                :screen s
                :height (if (util.is_vader) 30 50)
                :bg beautiful.bg_transparent}))

         ;; Add widgets to the wibox
         (s.top-bar:setup
          {:layout wibox.layout.flex.horizontal
           1 {:layout wibox.layout.fixed.horizontal
              1 mytextclock
              2 (-> (wibox.widget.systray)
                    ((fn [sys]
                       (set sys.forced_height 50)
                       sys)))
              3 separator
              4 (workrave)}
           2 {:layout wibox.container.place
              :valign "center"
              :halign "center"
              1 (org-clock)}
           3 {:layout wibox.layout.fixed.horizontal
              1 (spotify-widget)
              2 (when (util.is_vader) (batteryarc-widget))
              3 separator
              4 (dirty-repos)}})

         ;; Create the wibox
         (set s.bottom-bar
              (awful.wibar
               {:position "bottom"
                :screen s
                :height (if (util.is_vader) 70 100)
                :bg beautiful.bg_transparent}))

         ;; Add widgets to the wibox
         (s.bottom-bar:setup
          {:layout wibox.layout.align.horizontal
           1 nil
           2 {:layout wibox.container.place
              :valign "center"
              :halign "center"
              1 (workspaces.list)}
           3 nil})))))))
