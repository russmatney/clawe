(local wibox (require "wibox"))
(local lume (require "lume"))
(local icons (require "icons"))
(local helpers (require "dashboard.helpers"))

(local clawe (require "clawe"))

(local update-cb "update_workspaces_widget")
(fn request-updated-workspaces []
  (clawe.update-workspaces update-cb))

(fn text-color [wid {: selected : empty}]
  (if
   wid.hover "#ffffeeff"
   selected "#eeeeeeff"
   empty "#adadad99"
   "#bbbbbbdd"))

(fn icon-code [_wid {: fa_icon_code}]
  (or fa_icon_code "\u{f09b}"))

(fn icon-color [wid {: selected : empty}]
  (if
   wid.hover "#B9E3B7"
   selected "#d28343" ;; orange
   empty "#1b448C"  ;; blue
   ;; "#1a3b4C44"
   "#587D8D" ;; bluegreen
   ))

(fn make-wid-children [wid workspace]
  (let [{: awesome_index
         : key
         : name
         : scratchpad
         } workspace

        cont (wibox.widget {:layout wibox.layout.fixed.horizontal})
        ]

    (set wid.text-color (text-color wid workspace))
    (set wid.icon-code (icon-code wid workspace))
    (set wid.icon-color (icon-color wid workspace))

    (cont:set_children
     [(wibox.widget
       {:widget wibox.container.margin
        :margins 8
        1 (icons.make-fa-icon {:code wid.icon-code
                               :color wid.icon-color
                               :size 48})})
      (wibox.widget
       {:align "left"
        :markup (.. "<span color=\"" wid.text-color "\">"
                    (if (and scratchpad key)
                        key
                        (.. awesome_index ": "))
                    "</span>")
        :widget wibox.widget.textbox})
      (when (not scratchpad)
        (wibox.widget
         {:align "left"
          :markup (.. "<span color=\"" wid.text-color "\">"
                      (if scratchpad "" name) "</span>")
          :widget wibox.widget.textbox}))])

    ;; return container widget as list of children
    [cont]))

(fn make-workspace-widget [workspace]
  (let [wid (wibox.container.background)
        wid-children (make-wid-children wid workspace)]
    (wid:set_children wid-children)
    wid))

(fn attach-callbacks [wid workspace]
  (wid:connect_signal
   "button::press"
   (fn []
     (pp workspace)
     (helpers.tag_back_and_forth workspace.awesome_index)
     (request-updated-workspaces)
     ))

  (wid:connect_signal
   "mouse::enter" (fn []
                    ;; (tset wid :bg "#d2834399")
                    (tset wid :hover true)
                    (wid:set_children
                     (make-wid-children wid workspace))))
  (wid:connect_signal
   "mouse::leave" (fn []
                    ;; (tset wid :bg nil)
                    (tset wid :hover nil)
                    (wid:set_children
                     (make-wid-children wid workspace))))

  ;; must return wid!
  wid)

(local widget [])

(var workspaces-list (wibox.layout.fixed.horizontal))

(set widget.widget
     (wibox.widget
      {:widget wibox.container.background
       1 {:layout wibox.container.margin
          1 workspaces-list}}))

(tset
 _G update-cb
 (fn [workspaces]
   "Expects a list of objs with a :name key."
   (: workspaces-list :set_children
      (-> workspaces
          (lume.map (fn [wspc]
                      (-> wspc
                          make-workspace-widget
                          (attach-callbacks wspc))))))))

(fn worker []
  ;; called when module is created
  (request-updated-workspaces)

  widget.widget)

(setmetatable widget {:__call (fn [_ ...] (worker ...))})
