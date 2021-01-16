(local wibox (require "wibox"))
(local awful (require "awful"))
(local lume (require "lume"))
(local icons (require "icons"))

(local clawe (require "clawe"))

(fn make-workspace-widget [workspace]
  (let [{: awesome_index
         : key
         : name
         : fa_icon_code
         : scratchpad
         : selected
         : empty
         } workspace
        text-color (if empty
                       "#adadad99"
                       selected
                       "#eeeeeeff"
                       "#bbbbbbdd")
        icon-code (or fa_icon_code "\u{f09b}")
        icon-color (if selected
                       "#d28343" ;; orange
                       empty
                       "#1b448C" ;; blue
                       ;; "#1a3b4C44"
                       "#587D8D" ;; bluegreen
                       )
        ]
    (pp workspace)
    (wibox.widget
     {:widget wibox.container.background
      1 {:layout wibox.layout.fixed.horizontal
         1 (icons.make-fa-icon {:code icon-code :color icon-color :size 36})
         2 {:align "left"
            :markup (.. "<span color=\"" text-color "\">"
                        (if (and scratchpad key)
                            key
                            (.. awesome_index ": "))
                        "</span>")
            :widget wibox.widget.textbox}
         3 (when (not scratchpad)
             {:align "left"
              :markup (.. "<span color=\"" text-color "\">"
                          (if scratchpad "" name) "</span>")
              :widget wibox.widget.textbox})}})))

;; (wibox.widget
;;  {:layout wibox.layout.fixed.horizontal
;;   :set_count
;;   (fn [self new-value]
;;     (local str (.. "<span size=\"large\" font_weight=\"bold\" color=\"#efaefb\">"
;;                    new-value "</span>"))
;;     (set self.txt.markup str))
;;   1 {:align "center"
;;      :markup (.. "<span size=\"large\" font_weight=\"bold\" color=\"#536452\">"
;;                  "Workspaces: </span>")
;;      :widget wibox.widget.textbox}
;;   2 {:id "txt"
;;      :widget wibox.widget.textbox}
;;   3 icons.fa-timeicon})

(local widget [])

(var workspaces-list (wibox.layout.fixed.horizontal))

(set widget.widget
     (wibox.widget
      {:widget wibox.container.background
       1 {:layout wibox.container.margin
          1 workspaces-list}}))

(local update-cb "update_workspaces_widget")

(tset
 _G update-cb
 (fn [workspaces]
   "Expects a list of objs with a :name key."
   (: workspaces-list :set_children
      (lume.map workspaces make-workspace-widget))))

(fn worker []
  (: widget.widget :buttons
     (awful.util.table.join
      ;; click to force update
      (awful.button [] 1 (fn [] (clawe.update-workspaces update-cb)))))

  ;; called when module is created
  (clawe.update-workspaces update-cb)

  widget.widget)

(setmetatable widget {:__call (fn [_ ...] (worker ...))})
