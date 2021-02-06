(local wibox (require "wibox"))
(local lume (require "lume"))
(local awful (require "awful"))

(local workspace-meta-widget [])

(set
 workspace-meta-widget.widget
 (wibox.widget
  {:layout wibox.layout.fixed.horizontal
   :set_label
   (fn [self new-value]
     (set self.name.markup
          (.. "<span size=\"xx-large\" font_weight=\"bold\" color=\"#efaefb\">"
              new-value "</span>")))
   1 {:id "name"
      :widget wibox.widget.textbox}}))

(local
 update-widget
 (fn [_tag]
   (let [tags (-?> (awful.screen.focused)
                   (. :tags)
                   (lume.filter #(. $ :selected)))
         name
         (if
          (and tags (= 0 (# tags))) "No tag"
          (and tags (-> tags
                        (lume.map (fn [t] (.. t.name " / ")))
                        table.concat
                        (string.sub 1 -4))))

         app-str (and client client.focus client.focus.instance)
         name (.. (or name "")
                  (or (and app-str  (.. " - " app-str))
                      ""))]
     (when name
       (: workspace-meta-widget.widget :set_label name)))))

(fn worker []
  (tag.connect_signal "property::selected" update-widget)
  (client.connect_signal "focus" update-widget)
  (update-widget tag)

  workspace-meta-widget.widget)

(setmetatable workspace-meta-widget {:__call (fn [_ ...] (worker ...))})
