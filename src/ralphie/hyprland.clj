(ns ralphie.hyprland
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; raw helpers

(defn hc-raw!
  "fires hyprctl, returns the output"
  [msg]
  (let [cmd (str "hyprctl " msg)]
    (println cmd)
    (-> (process/process
          {:cmd cmd :out :string})
        process/check
        :out
        )))

;; TODO support batching... via some magic
;; hyprctl --batch "keyword general:border_size 2 ; keyword general:gaps_out 20"

(comment
  (-> (process/process
        {:cmd "hyprctl" :out :string})
      process/check
      :out
      )
  (hc-raw! "")
  (hc-raw! "--help")
  )

(defn hc!
  "fires hyprctl, returns edn"
  [msg]
  (-> (hc-raw! (str "-j " msg))
      (json/parse-string
        (fn [k] (keyword "hypr" k)))))

(defn issue-dispatch
  " <dispatcher> [args] → Issue a dispatch to call a keybind dispatcher with arguments"
  [dispatcher args]
  (hc-raw! (str "dispatch " dispatcher " " args)))

(defn issue-plugin
  "... - Issue a plugin request"
  [args]
  (hc! (str "plugin " args)))

(defn issue-keyword
  " <name> <value> → Issue a keyword to call a config keyword dynamically"
  [name value]
  (hc-raw! (str "keyword " name " " value)))

(defn issue-hyprpaper
  "... - Issue a hyprpaper request"
  [args]
  (hc! (str "hyprpaper " args)))

(defn issue-hyprsunset
  "... - Issue a hyprsunset request"
  [args]
  (hc! (str "hyprsunset " args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reload

(defn reload
  " [config-only] → Issue a reload to force reload the config. Pass 'config-only' to disable monitor reload"
  ([] (reload false))
  ([config-only]
   (hc-raw! (str "reload " (when config-only "config-only")))))

(comment
  (reload))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; models

(defn ->workspace [wsp]
  (assoc wsp
         :special? (when (:hypr/name wsp)
                     (string/includes? (:hypr/name wsp) "special:"))))

(defn ->client [cli]
  (assoc cli
         :workspace-name (some-> cli :hypr/workspace :hypr/name)
         :workspace-id (some-> cli :hypr/workspace :hypr/id)))

(comment
  (hc! "workspaces")
  (hc-raw! "-j workspaces")
  (hc-raw! "help"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public api

(defn version
  "Prints the hyprland version, meaning flags, commit and branch of build."
  []
  (hc! "version"))

(defn notify
  "... - Sends a notification using the built-in Hyprland notification system"
  ([msg] (notify msg nil))
  ([msg {:keys [] :as opts}]
   (let [msg   (-> msg (string/replace #"-" ""))
         icon  (-> opts ((some-fn :icon :level))
                   ((fn [icon]
                      (cond
                        (nil? icon)           5
                        (#{:warning 0} icon)  0
                        (#{:info 1} icon)     1
                        (#{:hint 2} icon)     2
                        (#{:error 3} icon)    3
                        (#{:confused 4} icon) 4
                        (#{:ok 5} icon)       5
                        (#{:none 6} icon)     6))))
         color (:color opts icon)
         til   (:til opts 5000)]
     (hc-raw! (str "notify " icon " " til " " color " " msg)))))

(comment
  (notify "hi")
  (notify "hi (friend) -") ;; apparently can't handle `-` in messages
  (notify "hi" {:level :ok})
  (notify "hi" {:level :warning})
  (notify "hi" {:level :info})
  (notify "hi" {:level :hint})
  (notify "hi" {:level :error})
  (notify "hi" {:level :confused})
  (notify "hi" {:level :none}))

(defn dismiss-notify
  "[amount] - Dismisses all or up to AMOUNT notifications"
  [amount]
  (hc! (str "dismissnotify " amount)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clients/windows

(defn get-active-window
  "Gets the active window name and its properties"
  []
  (->client
    (hc! "activewindow")))

(defn list-clients
  "Lists all windows with their properties"
  []
  (->>
    (hc! "clients")
    (map ->client)))

(defn set-prop
  "... - Sets a window property"
  [args]
  (hc-raw! (str "setprop " args)))

(defn add-tag-to-current [tag]
  (issue-dispatch "tagwindow" (str "+" tag)))
(defn remove-tag-from-current [tag]
  (issue-dispatch "tagwindow" (str "-- -" tag)))

(defn get-current-tags []
  (into #{} (:hypr/tags (get-active-window))))

(comment
  (get-current-tags)
  (add-tag-to-current "yoooo")
  (remove-tag-from-current "yoooo")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces

(defn get-active-workspace
  "Gets the active workspace and its properties"
  []
  (->workspace (hc! "activeworkspace")))

(defn list-workspaces
  "Lists all workspaces with their properties"
  []
  (->> (hc! "workspaces") (map ->workspace)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; monitors

(defn list-monitors
  "Lists active outputs with their properties"
  []
  (hc! "monitors"))

(defn list-monitors-all
  "List all outputs with their properties, both active and inactive"
  []
  (hc! "monitors all"))

(comment
  (notify "\n\nwhat up \n\n\nfrom deep in HYPRLAND!!!\n\n"))

(defn output
  "... - Allows you to add and remove fake outputs to your preferred backend"
  [args]
  (hc! (str "output " args)))

(defn list-devices
  "Lists all connected keyboards and mice"
  []
  (hc! "devices"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; binds/keyboard

(defn list-binds
  "Lists all registered binds"
  []
  (hc! "binds"))

(defn list-global-shortcuts
  "Lists all global shortcuts"
  []
  (hc! "globalshortcuts"))

(defn switch-xkb-layout
  "Sets the xkb layout index for a keyboard"
  [idx]
  (hc! (str "switchxkblayout " idx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cursor

(defn get-cursor-pos
  "Gets the current cursor position in global layout coordinates"
  []
  (hc! "cursorpos"))

(defn set-cursor
  "<theme> <size> - Sets the cursor theme and reloads the cursor manager"
  [theme size]
  (hc! (str "setcursor " theme " " size)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; other stuff

(defn get-animations
  "Gets the current config'd info about animations and beziers"
  []
  (hc! "animations"))

(defn list-decorations
  "<window_regex> - Lists all decorations and their info"
  [window-regex]
  (hc! (str "decorations " window-regex)))

(defn list-config-errors
  "Lists all current config parsing errors"
  []
  (hc! "configerrors"))

(defn set-error
  "<color> <message...> - Sets the hyprctl error string. Color has the same format as in colors in config. Will reset when Hyprland's config is reloaded"
  [color message]
  (hc! (str "seterror " color " " message)))

(defn get-option
  "<option> - Gets the config option status (values)"
  [opt]
  (hc! (str "getoption " opt)))

(defn list-instances
  "Lists all running instances of Hyprland with their info"
  []
  (hc! "instances"))

(defn issue-kill-mode
  "Issue a kill to get into a kill mode, where you can kill an app by clicking on it.
  You can exit it with ESCAPE"
  []
  (hc! "kill"))

(defn list-layers
  "Lists all the surface layers"
  []
  (hc! "layers"))

(defn list-layouts
  "Lists all layouts available (including plugin'd ones)"
  []
  (hc! "layouts"))

(defn rolling-log
  "Prints tail of the log. Also supports -f/--follow option"
  []
  (-> (hc-raw! "rollinglog")
      (string/split #"\n")))

(defn get-splash
  "Get the current splash"
  []
  (hc! "splash"))

(defn get-system-info
  "Get system info"
  []
  (hc-raw! "systeminfo"))

(defn list-workspace-rules
  "Lists all workspace rules"
  []
  (hc! "workspacerules"))

(comment
  (list-workspaces)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces

(defn current-wsp-id []
  (:hypr/id (get-active-workspace)))

(defn create-workspace [{:keys [name]}]
  (issue-dispatch "workspace" "emptyn") ;; next empty wsp
  (issue-dispatch "renameworkspace" (str (current-wsp-id) " " name)))

(defn focus-workspace [{:keys [id]}]
  (issue-dispatch "workspace" id))

(defn get-workspace [{:keys [name]}]
  (->>
    (list-workspaces)
    (filter
      (comp #{name (str "special:" name)}
            :hypr/name))
    first))

(comment
  (->>
    (list-workspaces)
    (map :hypr/name))
  (get-workspace {:name "journalemacs"})
  (get-workspace {:name "clawe"})
  (create-workspace {:name "dino"})
  (focus-workspace {:id 1}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clients

(defn get-client [{:keys [class]}]
  (->>
    (list-clients)
    (filter (comp #{class} string/lower-case :hypr/class))
    first))

(defn focus-client [{:keys [hypr/pid]}]
  (issue-dispatch "focuswindow" (str "pid:" pid)))

(defn close-client [{:keys [hypr/pid]}]
  (issue-dispatch "closewindow" (str "pid:" pid)))

(defn move-client-to-workspace [{:keys [hypr/pid]} {:keys [hypr/id]}]
  ;; NOTE only works for numbered (read: non-dynamic/named) workspaces
  (issue-dispatch "movetoworkspacesilent" (str id ",pid:" pid)))

(comment
  (list-clients)
  (get-client {:class "alacritty"})
  (focus-client (get-client {:class "alacritty"}))
  (move-client-to-workspace
    (get-client {:class "alacritty"})
    ;; does NOT work for negative wsp ids
    {:hypr/id -1337}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; floating
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-floating
  ([] (issue-dispatch "setfloating" nil))
  ([float?]
   (if float?
     (issue-dispatch "setfloating" nil)
     (do
       (issue-dispatch "setfloating" nil)
       (issue-dispatch "togglefloating" nil)))))

(defn toggle-floating
  [] (issue-dispatch "togglefloating" nil))

(defn center-client []
  (issue-dispatch "centerwindow" "1"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; resize client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn resize-client
  "Resize the focused window.
  :x and :y are strings - number of pixels \"300\", or a % \"50%\".
  Set :relative? to switch relative to the current window.
  "
  [{:keys [relative? x y]}]
  (issue-dispatch "resizeactive"
                  (str (when-not relative? "exact")
                       " " x " " y)))
(comment
  (resize-client {:relative? false
                  :x         "70%"
                  :y         "50%"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; move client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-client
  "Move the focused window.
  :x and :y are strings - number of pixels \"300\", or a % \"50%\".
  Set :relative? to switch relative to the current window.
  "
  [{:keys [relative? x y]}]
  (issue-dispatch "moveactive"
                  (str (when-not relative? "exact")
                       " " x " " y)))
(comment
  (move-client {:relative? false
                :x         "800"
                :y         "200"}))
