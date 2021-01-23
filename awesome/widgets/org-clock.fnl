(local wibox (require "wibox"))
(local gears (require "gears"))
(local spawn (require "awful.spawn"))

(local UPDATE_SCRIPT
       "emacsclient -e \"(russ/current-clock-string)\"")

(local org-clock-widget [])

(set org-clock-widget.widget
     (wibox.widget
      {:layout wibox.layout.fixed.horizontal
       :set_label
       (fn [self new-value]
         (set self.txt.markup
              (.. "<span size=\"xx-large\" font_weight=\"bold\" color=\"#efaefb\">"
                  new-value "</span>")))
       1 {:align "center"
          :widget wibox.widget.textbox}
       2 {:id "txt"
          :widget wibox.widget.textbox}}))

(fn _G.update_org_clock_widget [str]
  (when str
    (let [str
          ;; removes surrounding quotes in emacs output
          (string.sub str 2 (- (string.len str) 2))]
      (: org-clock-widget.widget :set_label str))))

(fn worker []
  (gears.timer
   {:timeout 20
    :call_now true
    :autostart true
    :callback
    (fn []
      (spawn.easy_async UPDATE_SCRIPT _G.update_org_clock_widget))})

  org-clock-widget.widget)

(setmetatable org-clock-widget {:__call (fn [_ ...] (worker ...))})
