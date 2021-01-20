(local awful (require "awful"))
(local beautiful (require "beautiful"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Manage Signal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn init_manage_signal []
  (_G.client.connect_signal
   "manage"
   (fn [c]
     ;; Set the windows as the slave,
     ;; i.e. put it at the end of others instead of setting it master.
     ;; if not awesome.startup then awful.client.setslave(c) end
     (if (and _G.awesome.startup
              (not c.size_hints.user_position)
              (not c.size_hints.program_position))
         (awful.placement.no_offscreen c)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Focus/Unfocus Signal
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fn init_focus_signals []
  (_G.client.connect_signal
   "focus"
   (fn [c] (set c.border_color beautiful.border_focus)))

  (_G.client.connect_signal
   "unfocus"
   (fn [c] (set c.border_color beautiful.border_normal))))


{: init_focus_signals
 : init_manage_signal}
