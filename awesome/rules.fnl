(local gears (require "gears"))
(local awful (require "awful"))
(local beautiful (require "beautiful"))

(local bindings (require :bindings))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rules
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Rules to apply to new clients (through the "manage" signal).
(local global_rules
       (gears.table.join
        [
         ;; maybe
         {:rule {}
          :properties
          {:border_width beautiful.border_width
           :border_color beautiful.border_normal
           :focus awful.client.focus.filter
           :raise true
           :keys bindings.clientkeys
           :buttons bindings.clientbuttons
           :screen awful.screen.preferred
           :placement (+ awful.placement.no_overlap
                         awful.placement.no_offscreen)}}

         ;; Floating clients.
         {:rule_any
          {:instance ["DTA" "copyq" "pinentry"]
           :class ["Arandr" "MessageWin" "Sxiv" "Wpa_gui"
                   "Blueman-manager" "Gpick" "Kruler"
                   "Tor Browser" "veromix" "xtightvncviewer"]
           :name ["Event Tester"]
           :role ["pop-up" "AlarmWindow" "ConfigManager"]}
          :properties {:floating true}}

         ;; Add titlebars to normal clients and dialogs
         {:rule_any {:type ["normal" "dialog"]}
          :properties {:titlebars_enabled false}}

         {:rule_any {:class ["Rofi" "rofi"]}
          :properties {:floating true
                       :placement awful.placement.centered
                       :titlebars_enabled false
                       :sticky true
                       :ontop true
                       :above true}}

         ;; handle org protocol/emacs popups
         {:rule_any {:name ["org-capture-frame" "doom-capture"]}
          :properties {
                       ;; :titlebars_enabled false
                       :floating true
                       :ontop true
                       :width     1600
                       :height    800
                       :placement awful.placement.centered}}

         {:rule_any {:class ["clover/twitch-chat"
                             "tauri/twitch-chat"]
                     :name ["Chat Box Widget"
                            "clover/twitch-chat"
                            "tauri/twitch-chat"]}
          :properties {:tag "journal"
                       :width 600
                       :height 1000
                       :above true
                       :ontop true
                       :sticky true
                       :floating true
                       :focusable false
                       }}

         {:rule {:name "Tauri App"}
          :properties {:floating true
                       :focus false}}

         {:rule {:name "tauri-doctor-topbar"}
          :properties
          {:tag "journal"
           :sticky true
           :above true
           :ontop true
           :focusable false
           :maximized_horizontal true
           :height 40
           :y 0
           :honor_padding false
           :honor_workarea false}}

         ;; ff fix
         {:rule {:class "firefox"}
          :properties {:maximized false
                       :floating false}}

         ;; youtube fix
         {:rule {:instance "plugin-container"}
          :properties {:floating true}}
         {:rule {:instance "exe"}
          :properties {:floating true}}

         ;; godot fix?
         {:rule_any {:name ["Godot Engine"]}
          :properties
          {:fullscreen false
           :maximized false
           :maximized_vertial false
           :maximized_horizontal false}
          :callback (fn [c]
                      (print "godot editor hit!")
                      (pp c)
                      (set c.fullscreen false)
                      (set c.maximized false)
                      (set c.maximized_vertical false)
                      (set c.maximized_horizontal false)
                      ;; (-> c
                      ;;     (tset :floating true)
                      ;;     ((+ awful.placement.scale
                      ;;         awful.placement.centered)
                      ;;      {:honor_padding true
                      ;;       :honor_workarea true
                      ;;       :to_percent 0.9}))
                      )}

         ;; fullscreen fix?
         {:rule_any {:instance ["_NET_WM_STATE_FULLSCREEN"]}
          :callback (fn [c]
                      (print "fullscreen client!")
                      (pp c)
                      (set c.fullscreen false)
                      (set c.maximized false)
                      (set c.maximized_vertical false)
                      (set c.maximized_horizontal false)
                      (-> c
                          (tset :floating true)
                          ((+ awful.placement.scale
                              awful.placement.centered)
                           {:honor_padding true
                            :honor_workarea true
                            :to_percent 0.9})))}]))

(fn init_rules []
  (set awful.rules.rules global_rules))

{: init_rules}
