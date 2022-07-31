(ns clawe.defs.workspaces
  (:require
   [clojure.string :as string]
   [defthing.defworkspace :as defworkspace :refer [defworkspace]]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]))

(def home-dir #zsh/expand "~")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc workspace builder helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move to clawe.awesome or clawe.rules
;; should calc at rules-write-time, not defworkspace-macro-time
(defn awm-workspace-rules
  "Returns values intended as literal awesome/rules.
  The first arg is always expected to be the relevant workspace.

  Supports two versions:
  - Single arity returns a simple rule.
  - Multi-arity implies a broader match-any."
  ([name]
   {:rule       {:name name}
    :properties {:tag name}})
  ([name & aliases]
   (let [all (cons name aliases)]
     {:rule_any   {:class all :name all}
      :properties {:tag name :first_tag name}})))

(defn awesome-rules [& args]
  (let [{:keys [name] :as thing} (last args)
        args                     (butlast args)]
    (assoc thing :awesome/rules (apply awm-workspace-rules name args))))

(comment
  (awesome-rules {:name "my-thing-name"})
  (awesome-rules "onemore" {:name "my-thing-name"})
  (awesome-rules "more" "andmore" {:name "my-thing-name"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify, Web, other app-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace messages
  {:workspace/scratchpad true})

(defworkspace spotify
  {:awesome/rules
   (awm-workspace-rules "spotify"  "Spotify" "Pavucontrol" "pavucontrol")}
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
  {:awesome/rules
   (awm-workspace-rules "audacity")}
  {:workspace/scratchpad       true
   :workspace/scratchpad-class "audacity"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"audacity"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace slack
  {:awesome/rules
   (awm-workspace-rules "slack")}
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
  {:awesome/rules
   (awm-workspace-rules "discord")}
  {
   :workspace/scratchpad       true
   :workspace/scratchpad-class "discord"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"discord"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace web
  {:awesome/rules
   (awm-workspace-rules "firefox")}
  {:workspace/exec             "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "firefox"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"firefox" "web"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace dev-browser
  {:awesome/rules
   (awm-workspace-rules "dev-browser" "chrome" "Chrome" "Firefox Developer Edition"
                        "firefoxdeveloperedition")}
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
       (or (matches name) (matches class))))
   :rules/apply
   (fn []
     (let [x (awm/client-for-class "firefoxdeveloperedition")
           y (awm/client-for-class "chrome")]
       (when y
         (notify/notify "Found chrome client, moving to dev-browser workspace" y)
         ;; TODO create workspace if it doesn't exist
         (awm/ensure-tag "dev-browser")
         (awm/move-client-to-tag (:awesome.client/window y) "dev-browser"))
       (when x
         (notify/notify "Found firefoxdeveloper client, moving to dev-browser workspace" x)
         ;; TODO create workspace if it doesn't exist
         (awm/ensure-tag "dev-browser")
         (awm/move-client-to-tag (:awesome.client/window x) "dev-browser"))))})

(defworkspace obs
  {:workspace/directory "russmatney/obs-recordings"}
  {:rules/is-my-client?
   (fn [c]
     (-> c :awesome.client/class #{"obs"}))})

(defworkspace pixels
  {:workspace/directory          "Dropbox/pixels"
   :workspace/initial-file       "readme.org"
   :workspace/scratchpad-classes #{"Aseprite"}
   :workspace/exec               "/usr/bin/gtk-launch aseprite"}
  {:rules/apply (fn []
                  (let [client (awm/client-for-name "Aseprite")]
                    (when client
                      (awm/ensure-tag "pixels")
                      (awm/move-client-to-tag (:awesome.client/window client) "pixels"))))})

(defworkspace steam
  {:rules/apply
   (fn []
     (let [steam-client (awm/client-for-name "Steam")]
       (when steam-client
         (notify/notify
           "Found slack call client, moving to steam workspace"
           steam-client)
         (awm/ensure-tag "steam")
         (awm/move-client-to-tag (:awesome.client/window steam-client) "steam"))))})

(defworkspace zoom
  {:awesome/rules (awm-workspace-rules "zoom" "Zoom" "Zoom Meeting" "Slack call")}
  {:rules/apply (fn []
                  (let [slack-call (awm/client-for-name "Slack call")]
                    (when slack-call
                      (notify/notify
                        "Found slack call client, moving to zoom workspace"
                        slack-call)
                      (awm/ensure-tag "zoom")
                      (awm/move-client-to-tag (:awesome.client/window slack-call) "zoom"))))
   :rules/is-my-client?
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
  {:awesome/rules (awm-workspace-rules "1password" "1Password")}
  {:rules/is-my-client?
   (fn [c]
     (let [matches                             #{"1password" "1Password"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))
   :rules/apply (fn []
                  (let [c (awm/client-for-name "1Password")]
                    (when c
                      (notify/notify
                        "Found 1password client, moving to zoom workspace"
                        c)
                      (awm/ensure-tag "one-password")
                      (awm/move-client-to-tag (:awesome.client/window c) "one-password"))))}
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
