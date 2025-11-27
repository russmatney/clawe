(ns ralphie.hyprland
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]
   ))

(defn hc-raw! [msg]
  "fires hyprctl, returns the output"
  (let [cmd (str "hyprctl " msg)]
    (println cmd)
    (-> (process/process
          {:cmd cmd :out :string})
        process/check
        :out
        )))

(comment
  (-> (process/process
        {:cmd "hyprctl" :out :string})
      process/check
      :out
      )
  (hc-raw! "")
  (hc-raw! "--help")
  )

(defn hc! [msg]
  "fires hyprctl, returns edn"
  (-> (hc-raw! (str "-j " msg))
      (json/parse-string
        (fn [k] (keyword "hypr" k)))))


(comment
  (hc! "workspaces")
  (hc-raw! "-j workspaces")
  (hc-raw! "help"))

(defn version []
  "Prints the hyprland version, meaning flags, commit and branch of build."
  (hc! "version"))

(defn get-active-window []
  "Gets the active window name and its properties"
  (hc! "activewindow"))

(defn get-active-workspace []
  "Gets the active workspace and its properties"
  (hc! "activeworkspace"))

(defn get-animations []
  "Gets the current config'd info about animations and beziers"
  (hc! "animations"))

(defn list-binds []
  "Lists all registered binds"
  (hc! "binds"))

(defn list-clients []
  "Lists all windows with their properties"
  (hc! "clients"))

(defn list-config-errors []
  "Lists all current config parsing errors"
  (hc! "configerrors"))

(defn get-cursor-pos []
  "Gets the current cursor position in global layout coordinates"
  (hc! "cursorpos"))

(defn list-decorations [window-regex]
  "<window_regex> - Lists all decorations and their info"
  (hc! (str "decorations " window-regex)))

(defn list-devices []
  "Lists all connected keyboards and mice"
  (hc! "devices"))

(defn dismiss-notify [amount]
  "[amount] - Dismisses all or up to AMOUNT notifications"
  (hc! (str "dismissnotify " amount)))

(defn issue-dispatch [dispatcher args]
  " <dispatcher> [args] → Issue a dispatch to call a keybind dispatcher with arguments"
  (hc-raw! (str "dispatch " dispatcher " " args)))

(defn get-option [opt]
  "<option> - Gets the config option status (values)"
  (hc! "getoption " opt))

(defn list-global-shortcuts []
  "Lists all global shortcuts"
  (hc! "globalshortcuts"))

(defn issue-hyprpaper [args]
  "... - Issue a hyprpaper request"
  (hc! (str "hyprpaper " args)))

(defn issue-hyprsunset [args]
  "... - Issue a hyprsunset request"
  (hc! (str "hyprsunset " args)))

(defn list-instances []
  "Lists all running instances of Hyprland with their info"
  (hc! "instances"))

(defn issue-keyword [name value]
  " <name> <value> → Issue a keyword to call a config keyword dynamically"
  (hc! (str "keyword " name " " value)))

(defn issue-kill-mode []
  "Issue a kill to get into a kill mode, where you can kill an app by clicking on it.
You can exit it with ESCAPE"
  (hc! "kill"))

(defn list-layers []
  "Lists all the surface layers"
  (hc! "layers"))

(defn list-layouts []
  "Lists all layouts available (including plugin'd ones)"
  (hc! "layouts"))

(defn list-monitors []
  "Lists active outputs with their properties"
  (hc! "monitors"))

(defn list-monitors-all []
  "List all outputs with their properties, both active and inactive"
  (hc! "monitors all"))

(defn notify [args]
  "... - Sends a notification using the built-in Hyprland notification system"
  (hc-raw! (str "notify 5 5000 0 " args)))

(comment
  (notify "\n\nwhat up \n\n\nfrom deep in HYPRLAND!!!\n\n"))

(defn output [args]
  "... - Allows you to add and remove fake outputs to your preferred backend"
  (hc! (str "output " args)))

(defn issue-plugin [args]
  "... - Issue a plugin request"
  (hc! (str "plugin " args)))

(defn reload
  " [config-only] → Issue a reload to force reload the config. Pass 'config-only' to disable monitor reload"
  ([] (reload false))
  ([config-only]
   (hc-raw! (str "reload " (when config-only "config-only")))))

(comment
  (reload))

(defn rolling-log []
  "Prints tail of the log. Also supports -f/--follow option"
  (-> (hc-raw! "rollinglog")
      (string/split #"\n")))

(defn set-cursor [theme size]
  "<theme> <size> - Sets the cursor theme and reloads the cursor manager"
  (hc! (str "setcursor " theme " " size)))

(defn set-error [color message]
  "<color> <message...> - Sets the hyprctl error string. Color has the same format as in colors in config. Will reset when Hyprland's config is reloaded"
  (hc! (str "seterror " color " " message)))

(defn set-prop [args]
  "... - Sets a window property"
  (hc! (str "setprop" args)))

(defn get-splash []
  "Get the current splash"
  (hc! "splash"))

(defn switch-xkb-layout [idx]
  "Sets the xkb layout index for a keyboard"
  (hc! (str "switchxkblayout " idx)))

(defn get-system-info []
  "Get system info"
  (hc-raw! "systeminfo"))

(defn list-workspaces []
  "Lists all workspaces with their properties"
  (hc! "workspaces"))

(defn list-workspace-rules []
  "Lists all workspace rules"
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
    (filter (comp #{name} :hypr/name))
    first))

(comment
  (list-workspaces)
  (get-workspace {:name "journal"})
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
