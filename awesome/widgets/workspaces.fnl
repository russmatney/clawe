(local wibox (require "wibox"))
(local lume (require "lume"))
(local icons (require "icons"))
(local helpers (require "dashboard.helpers"))

(local util (require "util"))
(local clawe (require "clawe"))

(local update-cb "update_workspaces_widget")
(fn request-updated-workspaces []
  (clawe.update-workspaces update-cb))

(fn text-color [wid {: selected : empty : color}]
  (if
   wid.hover "#ffffeeff"
   selected "#eeeeeeff"
   empty "#adadad99"
   (or color "#bbbbbbdd")))

(fn icon-code [_wid {: fa_icon_code}]
  (or fa_icon_code "\u{f09b}"))

;; "#ac3232" ;; darker red
(fn icon-color [wid {: selected : empty : tag : color}]
  (if
   tag.urgent "#d95763" ;; softer red
   wid.hover "#B9E3B7"
   tag.selected "#d28343"
   selected "#d28343" ;; orange
   empty "#1b448C"  ;; blue
   ;; "#1a3b4C44"
   (or color "#587D8D"))) ;; bluegreen

(var show-scratchpad-names false)
(fn _G.toggle_show_scratchpad_names [val]
  (set show-scratchpad-names
       (if (= val nil)
           (not show-scratchpad-names) val)))

(fn make-wid-children [wid workspace]
  (let [{: new_index : _key : name : scratchpad : title_pango} workspace
        cont (wibox.widget
              {:layout wibox.layout.fixed.horizontal})]

    (set wid.text-color (text-color wid workspace))
    (set wid.icon-code (icon-code wid workspace))
    (set wid.icon-color (icon-color wid workspace))

    (cont:set_children
     [(wibox.widget
       {:widget wibox.layout.align.vertical
        1 (icons.make-fa-icon {:margins 8
                               :code wid.icon-code
                               :color wid.icon-color
                               :size (if (util.is_vader) 48 64)})})

      (when (and (or title_pango name)
                 (or (not scratchpad)
                     (and scratchpad show-scratchpad-names)))
        (wibox.widget
         {:widget wibox.container.margin
          :left 6
          :right 12
          1 {:align "left"
             :markup (.. "<span size=\"xx-large\" color=\"" wid.text-color "\">"
                         (if
                          title_pango title_pango
                          (.. name " (" new_index ")")) "</span>")
             :widget wibox.widget.textbox}}))])

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

(local list-widget [])

(var workspaces-list (wibox.layout.fixed.horizontal))

(set list-widget.widget
     (wibox.widget
      {:widget wibox.container.background
       :bg "#274244c9"
       1 {:layout wibox.container.margin
          :margins 10
          1 workspaces-list}}))

;; TODO create higher level namespace for this behavior/event
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
                       (let [tag (util.get_tag {:name wsp.name})]
                         (when tag (tset wsp :tag tag))))))

      ;; delete all but one empty workspace
      (-> workspaces
          (lume.filter (fn [wsp] wsp.empty))
          (#(lume.slice $ 2 (# $)))
          (lume.each (fn [{: tag}] (tag:delete))))

      ;; sets the order according to what was passed in
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

(fn list-worker []
  (let [cb (fn [_tag]
             (request-updated-workspaces))]
    ;; called when module is created
    (request-updated-workspaces)

    ;; TODO figure out how to disconnect signals properly
    ;; otherwise it doesn't seem like this running multiple times is too bad
    (tag.connect_signal "property::selected" cb)
    (tag.connect_signal "property::urgent" cb)

    list-widget.widget))

(setmetatable list-widget {:__call (fn [_ ...] (list-worker ...))})
