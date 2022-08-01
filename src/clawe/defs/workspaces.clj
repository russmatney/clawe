(ns clawe.defs.workspaces
  (:require
   [clojure.string :as string]
   [defthing.defworkspace :as defworkspace :refer [defworkspace]]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]))

(def home-dir (zsh/expand "~"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify, Web, other app-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace messages
  {:workspace/scratchpad true})

(defworkspace spotify
  {:workspace/app-names ["Spotify" "Pavucontrol" "pavucontrol"]}
  {:workspace/initial-file     ".config/spicetify/config.ini"
   :workspace/exec             "spotify"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Spotify"
   :rules/is-my-client?
   (fn [c]
     (let [matches
           #{"spotify" "Spotify"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace audacity
  {:workspace/app-names []}
  {:workspace/scratchpad       true
   :workspace/scratchpad-class "audacity"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"audacity"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace slack
  {:workspace/exec             "slack"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Slack"
   :rules/is-my-client?
   (fn [c]
     ;; TODO ignore 'Slack call' clients
     (let [matches                             #{"slack" "Slack"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace discord
  {:workspace/scratchpad       true
   :workspace/scratchpad-class "discord"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"discord"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace web
  {:workspace/app-names ["firefox"]}
  {:workspace/exec             "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "firefox"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"firefox" "web"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace dev-browser
  {:workspace/app-names ["dev-browser" "chrome" "Chrome" "Firefox Developer Edition"
                         "firefoxdeveloperedition"]}
  {:workspace/exec               "/usr/bin/gtk-launch firefox-developer-edition.desktop"
   :workspace/scratchpad         true
   ;; TODO normalize classes (punctuation, spacing, etc)
   :workspace/scratchpad-classes #{"Google-chrome" "firefoxdeveloperedition"}}
  {:rules/is-my-client?
   (fn [c]
     (let [matches
           #{"dev-browser" ;; emacs match? ;; include title by default?
             "chrome" "Chrome"
             "google-chrome"
             "Firefox Developer Edition" "firefoxdeveloperedition"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace obs
  {:workspace/directory "russmatney/obs-recordings"}
  {:rules/is-my-client?
   (fn [c]
     (-> c :awesome.client/class #{"obs"}))})

(defworkspace pixels
  {:workspace/directory          "Dropbox/pixels"
   :workspace/initial-file       "readme.org"
   :workspace/scratchpad-classes #{"Aseprite"}
   :workspace/exec               "/usr/bin/gtk-launch aseprite"})

(defworkspace steam)

(defworkspace zoom
  {:workspace/app-names ["zoom" "Zoom" "Zoom Meeting" "Slack call"]}
  {:rules/is-my-client?
   (fn [c]
     (let [matches                             #{"zoom" "Zoom" "Zoom Meeting" "Slack call"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class)
           (string/includes? name "Zoom")
           (string/includes? name "Slack call"))))}
  {:workspace/scratchpad         true
   :workspace/scratchpad-names   #{"Zoom Meeting"}
   :workspace/scratchpad-classes #{"zoom" "Zoom Meeting" "Slack"}})

(defworkspace one-password
  {:workspace/app-names ["1password" "1Password"]}
  {:rules/is-my-client?
   (fn [c]
     (let [matches                             #{"1password" "1Password"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  {:workspace/scratchpad         true
   :workspace/scratchpad-classes #{"1Password"}
   :workspace/exec               "/usr/bin/gtk-launch 1password.desktop"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org, mind-gardening, blogging, writing workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace journal
  {:workspace/directory        "Dropbox/todo"
   :workspace/initial-file     "journal.org"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"journal" "tauri/doctor-topbar"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  (fn [x] {:scratchpad/is-my-client? (:rules/is-my-client? x)}))

(defworkspace garden
  {:workspace/directory "Dropbox/todo/garden"})

(defworkspace ink
  {:workspace/directory "Dropbox/todo/ink"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Doctor apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doctor-todo
  {:workspace/directory       "Dropbox/todo"
   :workspace/exec            {:tmux.fire/cmd        (str "bb --config " home-dir "/russmatney/clawe/bb.edn todo")
                               :tmux.fire/session    "doctor-todo"
                               :tmux.fire/interrupt? true}
   :workspace/initial-file    "projects.org"
   :workspace/scratchpad      true
   :workspace/scratchpad-name "tauri/doctor-todo"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"tauri/doctor-todo"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace doctor-popup
  {:workspace/directory       "russmatney/clawe"
   :workspace/exec            {:tmux.fire/cmd        (str "bb --config " home-dir "/russmatney/clawe/bb.edn popup")
                               :tmux.fire/session    "doctor-popup"
                               :tmux.fire/interrupt? true}
   :workspace/initial-file    "bb.edn"
   :workspace/scratchpad      true
   :workspace/scratchpad-name "tauri/doctor-popup"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"tauri/doctor-popup"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(comment
  (tmux/fire (:workspace/exec doctor-popup)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emacs repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace emacs
  {:workspace/directory    ".doom.d"
   :workspace/initial-file "init.el"
   :git/check-status?      true})

(defworkspace doom-emacs
  {:workspace/color        "#aaee88"
   :workspace/directory    ".emacs.d"
   :workspace/initial-file "docs/index.org"})
