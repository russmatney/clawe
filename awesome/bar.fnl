(local wibox (require "wibox"))
(local beautiful (require "beautiful"))

(local awful (require "awful"))
(local util (require "util"))

(global
 init_bar
 (fn [bar]
   (awful.screen.connect_for_each_screen
    (fn [s]
      (util.log_if_error
       (fn []
         ;; remove if it exists already
         (if s.top-bar (s.top-bar:remove))
         (if s.bottom-bar (s.bottom-bar:remove))

         ;; create some buffer at the top
         (set s.top-bar (awful.wibar {:position "top" :screen s
                                      :height 20
                                      :bg beautiful.bg_transparent
                                      ;; :bg beautiful.bg_normal_semi
                                      }))
         (s.top-bar:setup)

         ;; still hanging on to this systray...
         (set s.bottom-bar (awful.wibar {:position "bottom" :screen s
                                         :height 25
                                         ;; :bg beautiful.bg_transparent
                                         :bg beautiful.bg_normal_semi
                                         }))
         (s.bottom-bar:setup
          {:layout wibox.layout.flex.horizontal
           1 (wibox.widget.systray)})))))))
