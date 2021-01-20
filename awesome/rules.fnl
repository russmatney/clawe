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
        [{:rule {}
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
          :properties {:titlebars_enabled true}}

         ;; handle org protocol/emacs popups
         {:rule_any {:name ["org-capture-frame" "doom-capture"]}
          :properties {
                       ;; :titlebars_enabled false
                       :floating true
                       :ontop true
                       :width     1600
                       :height    800
                       :placement awful.placement.centered}}

         {:rule_any {:class ["workrave" "Workrave"
                             "Rest break" "Micro break"]
                     :name ["workrave" "Workrave"
                            "Rest break" "Micro break"]}
          :properties {:floating true
                       :ontop true
                       :above true
                       :sticky true
                       :switch_to_tags true
                       :tag "workrave"
                       :new_tag "workrave"}}

         {:rule_any {:class ["clover/twitch-chat"]
                     :name ["Chat Box Widget"
                            "clover/twitch-chat"]}
          :properties {:width 600
                       :height 1000
                       :ontop true
                       :sticky true}}

         {:rule {:class "firefox"}
          :properties {:tag "web"
                       :maximized false
                       :floating false}}

         ;; youtube fix
         {:rule {:instance "plugin-container"}
          :properties {:floating true}}
         {:rule {:instance "exe"}
          :properties {:floating true}}

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
                            :to_percent 0.9}))
                      )}

         {:rule {:name "notes"}
          :properties {:tag "notes"}}
         {:rule {:name "journal"}
          :properties {:tag "journal"}}
         {:rule {:name "ralphie"}
          :properties {:tag "ralphie"}}
         {:rule {:name "org-crud"}
          :properties {:tag "org-crud"}}
         ;; TODO support arbitrary name <> tag for repos

         {:rule_any {:class ["Spotify" "spotify" "Pavucontrol" "pavucontrol"]
                     :name ["Spotify" "spotify" "Pavucontrol" "pavucontrol"]}
          :properties {:tag "spotify"
                       ;; TODO fix restart to not create duplicate workspaces
                       ;; :new_tag "spotify"
                       :switch_to_tags true
                       :first_tag "spotify"}}

         {:rule_any {:class ["Slack" "slack" "Discord" "discord"]
                     :name ["Slack" "slack" "Discord" "discord"]}
          :properties {:tag "slack"
                       ;; :new_tag "slack"
                       :first_tag "slack"
                       :switch_to_tags true}}]))

(fn init_rules []
  ;; TODO be nice to update changed rules, not _all_
  (set awful.rules.rules global_rules))

{: init_rules}
