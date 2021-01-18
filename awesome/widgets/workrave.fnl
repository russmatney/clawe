(local wibox (require "wibox"))
(local gears (require "gears"))
(local spawn (require "awful.spawn"))

(fn update-rest-break []
  (spawn.easy_async "clawe update-rest-break" (fn [_] nil)))

(fn update-micro-break []
  (spawn.easy_async "clawe update-micro-break" (fn [_] nil)))

(local workrave-widget [])

(set workrave-widget.widget
     (wibox.widget
      {:layout wibox.layout.fixed.horizontal
       :set_micro_break
       (fn [self new-value]
         (set self.micro.markup
              (if (= new-value 0)
                  "Micro break time!"
                  (.. "Micro break in "
                      "<span size=\"large\" font_weight=\"bold\" color=\"#efaefb\">"
                      new-value "</span> min."))))
       :set_rest_break
       (fn [self new-value]
         (set self.rest.markup
              (if (= new-value 0)
                  "Rest break time!"
                  (.. "Rest break in "
                      "<span size=\"large\" font_weight=\"bold\" color=\"#efaefb\">"
                      new-value "</span> min."))))
       1 {:id "micro"
          :widget wibox.widget.textbox
          :text "micro"}
       2 {:widget wibox.widget.textbox
          :text " | "}
       3 {:id "rest"
          :widget wibox.widget.textbox
          :text "rest"}}))

(fn _G.update_micro_break [t]
  (when t
    (: workrave-widget.widget :set_micro_break t)))

(fn _G.update_rest_break [t]
  (when t
    (: workrave-widget.widget :set_rest_break t)))

(var micro-timer nil)
(var rest-timer nil)

(fn worker []
  (when (and micro-timer micro-timer.started) (micro-timer:stop))
  (set micro-timer (gears.timer.start_new 20 update-micro-break))

  (when (and rest-timer rest-timer.started) (rest-timer:stop))
  (set rest-timer (gears.timer.start_new 20 update-rest-break))

  workrave-widget.widget)

(setmetatable workrave-widget {:__call (fn [_ ...] (worker ...))})
