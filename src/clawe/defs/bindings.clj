(ns clawe.defs.bindings
  (:require
   [clawe.defthing :as defthing]
   [clawe.awesome :as awm]
   [clawe.workspaces :as workspaces]
   [ralph.defcom :as defcom]
   [ralphie.notify :as notify]
   [ralphie.rofi :as rofi]
   [ralphie.term :as r.term]
   [ralphie.tmux :as r.tmux]
   [ralphie.zsh :as r.zsh]
   ;; TODO remove non bb deps from chess (clj-http)
   ;; [chess.core :as chess]
   ;; TODO require as first class dep
   ;; [systemic.core :as sys]

   [clojure.string :as string]
   [babashka.process :as process]
   [ralphie.emacs :as r.emacs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-bindings []
  (defthing/list-xs :clawe/bindings))

(defn get-binding [bd]
  (defthing/get-x :clawe/bindings
    (comp #{(:name bd bd)} :name)))

(defn binding-key->str [k]
  (let [mods (->> (first k) (apply str) (#(string/replace % #":" "")))
        k (second k)]
    (str mods k)))

(defn binding->defcom
  "Expands a binding to fulfill the defcom api."
  [{:keys [binding/key binding/command name] :as bd}]
  (let [nm (str name "-keybdg-" (binding-key->str key))
        bd (assoc bd
                  :defcom/name nm
                  :defcom/handler command
                  ;; an attempt to deal with defcom's 2 airty situation
                  ;; (fn [_ _] (command))
                  )]
    ;; not sure if this is enough - might fail in some cases (install-micro)
    (defcom/add-command (symbol nm) bd)
    bd))

(defmacro defbinding [title & args]
  (apply defthing/defthing :clawe/bindings title args))


(declare kbd)
(defmacro defbinding-kbd [title key-def command]
  `(defbinding ~title
    (kbd ~key-def ~command)
    binding->defcom))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Binding helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kbd [key command]
  (fn [x]
    (assoc x
           :binding/key key
           :binding/command command)))

(defn call-kbd [bd]
  ((:binding/command bd) nil nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defbinding-kbd toggle-floating
  [[:mod] "v"]
  (fn [_ _] (notify/notify "Toggling Floating!")))

(defbinding-kbd toggle-all-titlebars
  [[:mod :shift] "t"]
  (fn [_ _]
    (notify/notify "Toggling all titlebars!")
    (awm/awm-fnl
      '(->
         (client.get)
         (lume.each awful.titlebar.toggle)))))

(defbinding-kbd toggle-workspace-browser
  [[:mod] "b"]
  (fn [_ _]
    (notify/notify "toggling workspace browser")
    ;; get current workspace
    ;; determine if it has it's browser open already
    ;; toggle!
    (workspaces/current-workspace)))

(defbinding-kbd tile-all-windows
  [[:mod :shift] "f"]
  (fn [_ _]
    (notify/notify "tiling all windows")
    (awm/awm-fnl
      '(->
         (client.get)
         (lume.each (fn [c] (tset c :floating false)))))))

(defbinding-kbd open-chess-game
  [[:mod :shift] "e"]
  (fn [_ _]
    (notify/notify "Fetching chess games")
    ;; (sys/start! 'chess/*lichess-env*)
    (->>
      [] #_(chess/fetch-games)
      (map (fn [{:keys [lichess/url white-user black-user] :as game}]
             (assoc game
                    :rofi/label (str white-user " vs " black-user)
                    :rofi/url   url)))
      ;; TODO expand games into open-in-browser, open-in-garden options
      (rofi/rofi {:msg       "Open Game"
                  :on-select (fn [game]
                               (notify/notify "TODO impl open game"
                                              game))}))))

(defn reload-notification-css []
  (process/$
    notify-send.py a --hint boolean:deadd-notification-center:true
    string:type:reloadStyle))

(defbinding-kbd toggle-notifications-center
  [[:mod :alt] "n"]
  (fn [_ _]
    ;; (reload-notification-css)
    (let [deadd-pid (->
                      ^{:out :string}
                      (process/$ pidof deadd-notification-center)
                      process/check
                      :out
                      string/trim)]
      (-> (process/$ kill -s USR1 ~deadd-pid)
          process/check))))

(defbinding-kbd toggle-workspace-garden
  [[:mod :shift] "g"]
  (fn [_ _]
    (notify/notify "Toggling workspace garden")
    (let [{:workspace/keys [title]
           :awesome/keys   [clients]}
          (some->> [(workspaces/current-workspace)]
                   (workspaces/merge-awm-tags)
                   first)
          wsp-garden-title (str "garden-" title)
          open-client
          (->> clients (filter (comp #{wsp-garden-title} :name))
               first)]
      (cond
        (and open-client (:focused open-client))
        (awm/close-client open-client)
        (and open-client (not (:focused open-client)))
        (awm/set-focused open-client)
        (not open-client)
        (r.emacs/open
          {:emacs.open/workspace wsp-garden-title
           :emacs.open/file
           (r.zsh/expand (str "~/todo/garden/workspaces/" title ".org"))})))))

(defbinding-kbd toggle-terminal
  [[:mod] "Return"]
  (fn [_ _]
    (let [{:workspace/keys [title directory]} (workspaces/current-workspace)
          opts                                {:tmux/name      title
                                               :tmux/directory directory}]
      (notify/notify "Toggling Terminal" opts)
      (r.tmux/open-session opts))))

(defbinding-kbd toggle-emacs
  [[:mod :shift] "Return"]
  (fn [_ _]
    (let [{:workspace/keys
           [title initial-file]} (workspaces/current-workspace)
          initial-file           (or initial-file
                                     nil
                                     ;; TODO fall-back initial file
                                     )
          opts                   {:emacs.open/workspace title
                                  :emacs.open/file      initial-file
                                  }]
      (notify/notify "Toggling Emacs" opts)
      (r.emacs/open opts))))

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
