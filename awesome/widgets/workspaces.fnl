(local wibox (require "wibox"))
(local lume (require "lume"))
(local icons (require "icons"))
(local helpers (require "dashboard.helpers"))

(local util (require "util"))
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

(fn icon-color [wid {: selected : empty : tag}]
  (if
   wid.hover "#B9E3B7"
   tag.selected "#d28343"
   selected "#d28343" ;; orange
   empty "#1b448C"  ;; blue
   ;; "#1a3b4C44"
   "#587D8D" ;; bluegreen
   ))

(fn make-wid-children [wid workspace]
  (let [{: _awesome_index : _key : name : scratchpad} workspace
        cont (wibox.widget {:layout wibox.layout.fixed.horizontal})]

    (set wid.text-color (text-color wid workspace))
    (set wid.icon-code (icon-code wid workspace))
    (set wid.icon-color (icon-color wid workspace))

    (cont:set_children
     [(wibox.widget
       {:widget wibox.layout.align.vertical
        1 (icons.make-fa-icon {:margins 8
                               :code wid.icon-code
                               :color wid.icon-color
                               :size (if (util.is_vader) 48 64)})
        ;; 2 (wibox.widget
        ;;    {:align "center"
        ;;     :markup (.. "<span color=\"" wid.text-color "\">"
        ;;                 (if (and scratchpad key)
        ;;                     key
        ;;                     (.. awesome_index ": "))
        ;;                 "</span>")
        ;;     :widget wibox.widget.textbox})
        })
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

(local current-workspace-name
       (wibox.widget
        {:align "center"
         :widget wibox.widget.textbox}))

(fn set-current-workspace-name [wsp]
  (set current-workspace-name.markup
       (.. "<span size=\"large\" color=\"" "#edfedf" "\">"
           wsp.name "</span>")))

(tset
 _G update-cb
 (fn [workspaces]
   "Expects a list of objs with a :name key.
Sets all workspace indexes to match the passed :i."
   (util.log_if_error
    (fn []
      ;; set local awesome tag on each wsp
      (-> workspaces
          (lume.each (fn [wsp]
                       (let [tag (util.get_tag
                                  {:index wsp.awesome_index})]
                         (tset wsp :tag tag)

                         (when tag.selected
                           (set-current-workspace-name wsp))))))

      ;; disabled for now - this index overwriting needs more thought
      (-> workspaces
          (lume.each (fn [wsp]
                       (let [{:new_index i} wsp]
                         (util.move_tag_to_index wsp.tag i)))))

      (: workspaces-list :set_children
         (-> workspaces
             (lume.map (fn [wsp]
                         (-> wsp
                             make-workspace-widget
                             (attach-callbacks wsp))))))))))

(fn worker []
  (let [cb (fn [_tag]
             (request-updated-workspaces))]
    ;; called when module is created
    (request-updated-workspaces)

    ;; TODO figure out how to disconnect signals properly
    ;; otherwise it doesn't seem like this running multiple times is too bad
    (tag.connect_signal "property::selected" cb)

    widget.widget))

(local list-widget
       (setmetatable widget {:__call (fn [_ ...] (worker ...))}))

{:list list-widget
 :current-name current-workspace-name}
