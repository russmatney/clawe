(ns clawe.bindings
  (:require
   [clawe.defthing :as defthing]
   [ralphie.notify :as notify]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-bindings []
  (defthing/list-xs :clawe/bindings))

(defn get-binding [bd]
  (defthing/get-x :clawe/bindings
    (comp #{(some bd [:name identity])} :name)))

(defmacro defbinding [title & args]
  (apply defthing/defthing :clawe/bindings title args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding toggle-floating
  {:binding/key [[:mod] "f"]
   :binding/command (fn []
                      (notify/notify "Toggling Floating!"))})

(defn kbd [key command]
  (fn [x]
    (assoc x
           :binding/key key
           :binding/command command)))

(defbinding toggle-all-titlebars
  (kbd [[:mod :shift] "t"]
       (fn []
         (notify/notify "Toggling all titlebars!"))))

(comment
  ((:binding/command toggle-all-titlebars)))


;;    ;; walk tags
;;    (key [:mod] "Left" awful.tag.viewprev)
;;    (key [:mod] "Right" awful.tag.viewnext)

;;    ;; previous tag
;;    (key [:mod] "Escape" awful.tag.history.restore)

;;    ;; TODO move all these bindings into ralphie itself
;;    ;; ralphie rofi
;;    (key [:mod] "x" (spawn-fn "ralphie rofi"))

;;    ;; scratchpads
;;    ;; TODO should pull the letter from workspaces.org
;;    ;; and write into ralphie-build-and-install
;;    (key [:mod] "u" (spawn-fn "ralphie-toggle-scratchpad journal"))
;;    (key [:mod] "y" (spawn-fn "ralphie-toggle-scratchpad yodo-app"))
;;    (key [:mod] "g" (spawn-fn "ralphie-toggle-scratchpad notes"))
;;    (key [:mod] "t" (spawn-fn "ralphie-toggle-scratchpad web"))
;;    ;; (key [:mod] "b" (spawn-fn "ralphie-toggle-scratchpad chrome"))
;;    (key [:mod] "a" (spawn-fn "ralphie-toggle-scratchpad slack"))
;;    (key [:mod] "s" (spawn-fn "ralphie-toggle-scratchpad spotify"))

;;    ;; clawe keybindings
;;    (key [:mod] "r"
;;         (fn []
;;           ;; WARN potential race case on widgets reloading
;;           (awful.spawn "clawe rebuild-clawe" false)
;;           (awful.spawn "clawe reload" false)))

;;    (key [:mod] "d" (spawn-fn "clawe clean-workspaces"))
;;    (key [:mod] "o" (spawn-fn "clawe open-workspace"))
;;    (key [:mod] "w" (spawn-fn "clawe rofi"))

;;    ;; cycle layouts
;;    (key [:mod] "Tab"
;;         (fn []
;;           (let [scr (awful.screen.focused)]
;;             (awful.layout.inc 1 scr _G.layouts))))
;;    (key [:mod :shift] "Tab"
;;         (fn []
;;           (let [scr (awful.screen.focused)]
;;             (awful.layout.inc -1 scr _G.layouts))))

;;    ;; cycle workspaces
;;    (key [:mod] "n"
;;         (fn []
;;           (let [scr         (awful.screen.focused)
;;                 current-tag scr.selected_tag
;;                 idx         (if current-tag current-tag.index 1)
;;                 tag-count   (tablex.size scr.tags)
;;                 next-idx    (- idx 1)
;;                 next-idx    (if (< next-idx 1)
;;                               tag-count
;;                               next-idx)
;;                 next-tag    (. scr.tags next-idx)]
;;             (next-tag:view_only)
;;             (clawe.update-workspaces))))
;;    (key [:mod] "p"
;;         (fn []
;;           (let [scr         (awful.screen.focused)
;;                 current-tag scr.selected_tag
;;                 idx         (if current-tag current-tag.index 1)
;;                 tag-count   (tablex.size scr.tags)
;;                 next-idx    (+ idx 1)
;;                 next-idx    (if (> next-idx tag-count)
;;                               1
;;                               next-idx)
;;                 next-tag    (. scr.tags next-idx)]
;;             (next-tag:view_only)
;;             (clawe.update-workspaces))))

;;    (key [:mod :shift] "n" (spawn-fn "clawe drag-workspace-index down"))
;;    (key [:mod :shift] "p" (spawn-fn "clawe drag-workspace-index up"))

;;    ;; terminal
;;    (key [:mod] "Return"
;;         (fn []
;;           (let [current-tag (. (awful.screen.focused) :selected_tag)
;;                 name        current-tag.name
;;                 str         (.. "ralphie-open-term " name)]
;;             (awful.spawn str))))

;;    ;; emacs
;;    (key [:mod :shift] "Return" (spawn-fn "ralphie-open-emacs"))

;;    ;; launcher (rofi)
;;    (key [:mod] "space" (spawn-fn "/usr/bin/rofi -show drun -modi drun"))

;;    ;; finder (thunar)
;;    (key [:mod] "e" (spawn-fn "/usr/bin/thunar"))

;;    ;; screenshots
;;    (key [:mod :shift] "s" (spawn-fn "ralphie screenshot full"))
;;    (key [:mod :shift] "a" (spawn-fn "ralphie screenshot region"))

;;    ;; brightness
;;    (key [] "XF86MonBrightnessUp" (spawn-fn "light -A 5"))
;;    (key [] "XF86MonBrightnessDown" (spawn-fn "light -U 5"))

;;    ;; media controls
;;    ;; TODO play-pause should create spotify if its not open
;;    (key [] "XF86AudioPlay" (spawn-fn "spotifycli --playpause"))
;;    (key [] "XF86AudioNext" (spawn-fn "playerctl next"))
;;    (key [] "XF86AudioPrev" (spawn-fn "playerctl previous"))
;;    (key [] "XF86AudioMute" (spawn-fn "pactl set-sink-mute @DEFAULT_SINK@ toggle"))
;;    (key [] "XF86AudioRaiseVolume" (spawn-fn "pactl set-sink-volume @DEFAULT_SINK@ +5%"))
;;    (key [] "XF86AudioLowerVolume" (spawn-fn "pactl set-sink-volume @DEFAULT_SINK@ -5%"))

;;    ])
