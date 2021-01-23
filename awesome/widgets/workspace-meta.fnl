(local awful (require "awful"))
(local wibox (require "wibox"))

(local workspace-meta-widget [])

(set workspace-meta-widget.widget
     (wibox.widget
      {:layout wibox.layout.fixed.horizontal
       :set_label
       (fn [self new-value]
         (set self.name.markup
              (.. "<span size=\"xx-large\" font_weight=\"bold\" color=\"#efaefb\">"
                  new-value "</span>")))
       1 {:id "name"
          :widget wibox.widget.textbox}
       2 {:id "lay"
          :widget wibox.widget.textbox}}))

(fn update-widget [tag]
  (let [tag (if (and tag tag.selected) tag
                (-?> (awful.screen.focused)
                     (. :selected_tag)))
        name (if tag tag.name "No tag")]
    (when name
      (: workspace-meta-widget.widget :set_label name))))

(fn worker []
  (tag.connect_signal "property::selected" update-widget)
  (update-widget tag)

  workspace-meta-widget.widget)

(setmetatable workspace-meta-widget {:__call (fn [_ ...] (worker ...))})
