(local wibox (require "wibox"))
(local awful (require "awful"))
(local beautiful (require "beautiful"))
(local lume (require "lume"))
;; (local icons (require "icons"))

(local clawe (require "clawe"))

(fn make-workspace-widget [workspace]
  (pp "workspace-test-blah")
  (pp workspace)
  (wibox.widget
   {:bg beautiful.bg_transparent
    :widget wibox.container.background
    1 {:layout wibox.container.margin
       :margins 8
       1 {:layout wibox.layout.fixed.horizontal
          ;; TODO specify icon?
          :margins 16
          1 {:align "left"
             :text (.. "i: " workspace.awesome_index)
             :widget wibox.widget.textbox}
          2 {:align "left"
             :text workspace.name
             :widget wibox.widget.textbox}}}}))

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
      {:bg beautiful.bg_transparent
       :widget wibox.container.background
       1 {:layout wibox.container.margin
          :id "cont"
          :margins 8
          1 workspaces-list}}))

(local update-cb "update_workspaces_widget")

(tset
 _G update-cb
 (fn [workspaces]
   "Expects a list of objs with a :name key."
   (pp workspaces)
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
