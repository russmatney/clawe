(local wibox (require "wibox"))
(local gears (require "gears"))
(local spawn (require "awful.spawn"))

(local UPDATE_SCRIPT
       "emacsclient -e \"(russ/current-clock-string)\"")

(local UPDATE_VIA_CLAWE
       "clawe update-org-clock")

(local org-clock-widget [])

(set org-clock-widget.widget
     (wibox.widget
      {:layout wibox.layout.fixed.horizontal
       :set_label
       (fn [self new-value]
         (set self.txt.markup
              (.. "<span size=\"xx-large\" font_weight=\"bold\" color=\"#efaefb\">"
                  new-value "</span>")))
       1 {:id "txt"
          :widget wibox.widget.textbox}}))

(fn _G.update_org_clock_widget [str]
  (when str
    (: org-clock-widget.widget :set_label str)))

(fn worker []
  (gears.timer
   {:timeout 20
    :call_now true
    :autostart true
    :callback
    (fn []
      ;; (spawn.easy_async UPDATE_SCRIPT _G.update_org_clock_widget)
      (spawn.easy_async UPDATE_VIA_CLAWE (fn [_] nil)))})

  org-clock-widget.widget)

(setmetatable org-clock-widget {:__call (fn [_ ...] (worker ...))})
