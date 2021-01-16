(local wibox (require "wibox"))
(local beautiful (require "beautiful"))

(local dirty-repos-widget (require "widgets.dirty-repos"))
(local org-pomo-widget (require "widgets.org-pomodoro"))
(local workspaces-widget (require "widgets.workspaces"))

;; (local pomodoro-widget (require "awesome-wm-widgets.pomodoroarc-widget.pomodoroarc"))
(local batteryarc-widget (require"awesome-wm-widgets.batteryarc-widget.batteryarc"))
(local spotify-widget (require"awesome-wm-widgets.spotify-widget.spotify"))

(local awful (require "awful"))
(local util (require "util"))

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
      ;; remove if it exists already
      (if s.mywibox (s.mywibox:remove))

      ;; Create the wibox
      (set s.mywibox
           (awful.wibar {:position "bottom" :screen s
                         :bg beautiful.bg_transparent}))

      ;; Add widgets to the wibox
      (s.mywibox:setup
       {:layout wibox.layout.align.horizontal
        1  {:layout wibox.layout.fixed.horizontal ;; Left widgets
            1 (workspaces-widget)}

        ;; Middle widget
        2 {:layout wibox.layout.fixed.horizontal
           1 (spotify-widget)
           2 (when (util.is-vader) (batteryarc-widget))
           3 separator
           4 (dirty-repos-widget)
           5 separator
           6 (org-pomo-widget)
           }

        ;; Right widgets
        3 {:layout wibox.layout.fixed.horizontal
           1 separator
           2 (wibox.widget.systray)
           3 mytextclock}})))))
