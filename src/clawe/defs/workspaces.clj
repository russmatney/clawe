(ns clawe.defs.workspaces
  (:require
   [defthing.defworkspace :refer [defworkspace]]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [ralphie.tmux :as tmux]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc workspace builder helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  (awesome-rules "more" "andmore" {:name "my-thing-name"})
  )

;; TODO could move into defthing, but what to do about that /home/russ ? bb.process?
;; TODO think about workspaces with two repos, or composed of apps
(defn workspace-repo
  "
  Expects at least a :workspace/directory as a string relative to $HOME.

  {;; required
   :workspace/directory    \"russmatney/dotfiles\",

   ;; optional - defaults to readme.org
   :workspace/readme \"readme.org\"

   ;; optional - defaults to readme.org
   :workspace/initial-file \"todo.org\"}

  Sets a :git/repo and some :workspace keys as absolute paths.

  {:git/repo               \"/home/russ/russmatney/dotfiles\",
   :workspace/directory    \"/home/russ/russmatney/dotfiles\",
   :workspace/readme       \"/home/russ/russmatney/dotfiles/readme.org\"
   :workspace/initial-file \"/home/russ/russmatney/dotfiles/todo.org\"}
  "
  [{:workspace/keys [directory readme initial-file]}]
  (let [readme-path (or initial-file readme "readme.org")]
    {:git/repo               (str "/home/russ/" directory)
     :workspace/directory    (str "/home/russ/" directory)
     :workspace/readme       (str "/home/russ/" directory "/" readme-path)
     :workspace/initial-file (str "/home/russ/" directory "/" (or initial-file readme-path))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify, Web, other app-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace spotify
  {:awesome/rules
   (awm-workspace-rules "spotify"  "Spotify" "Pavucontrol" "pavucontrol")}
  {:workspace/directory        "."
   :workspace/initial-file     ".config/spicetify/config.ini"
   :workspace/exec             "spotify"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Spotify"
   :rules/is-my-client?
   (fn [c]
     (let [matches
           #{"spotify" "Spotify"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  workspace-repo)

(defworkspace slack
  {:awesome/rules
   (awm-workspace-rules "slack" "discord")}
  {:workspace/directory        "."
   :workspace/exec             "slack"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Slack"
   :rules/is-my-client?
   (fn [c]
     ;; TODO ignore 'Slack call' clients
     (let [matches                             #{"slack" "Slack"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  workspace-repo)

(defworkspace web
  {:awesome/rules
   (awm-workspace-rules "firefox")}
  {:workspace/directory        "."
   :workspace/exec             "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "firefox"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"firefox" "web"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  workspace-repo)

(defworkspace dev-browser
  {:awesome/rules
   (awm-workspace-rules "dev-browser" "chrome" "Chrome" "Firefox Developer Edition"
                        "firefoxdeveloperedition")}
  {:workspace/directory  "."
   :workspace/exec       "/usr/bin/gtk-launch firefox-developer-edition.desktop"
   :workspace/scratchpad true
   ;; TODO normalize classes (punctuation, spacing, etc)
   :workspace/scratchpad-classes #{"Google-chrome" "firefoxdeveloperedition"}}
  {:rules/is-my-client?
   (fn [c]
     (let [matches
           #{"dev-browser" ;; emacs match? ;; include title by default?
             "chrome" "Chrome"
             "google-chrome"
             "Firefox Developer Edition" "firefoxdeveloperedition" }
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
         (awm/move-client-to-tag (:awesome.client/window x) "dev-browser"))))}
  workspace-repo)

(defworkspace obs
  {:workspace/directory "russmatney/obs-recordings"}
  {:rules/is-my-client?
   (fn [c]
     (-> c :awesome.client/class #{"obs"}))}
  workspace-repo)

(defworkspace pixels
  {:workspace/directory          "Dropbox/pixels"
   :workspace/initial-file       "readme.org"
   :workspace/scratchpad-classes #{"Aseprite"}
   :workspace/exec               "/usr/bin/gtk-launch aseprite"}
  {:rules/apply (fn []
                  (let [client (awm/client-for-name "Aseprite")]
                    (when client
                      (awm/ensure-tag "pixels")
                      (awm/move-client-to-tag (:awesome.client/window client) "pixels")))
                  )}
  workspace-repo)

(defworkspace tiles
  {:workspace/directory    "/home/russ/Dropbox/tiles"
   :workspace/initial-file "/home/russ/Dropbox/tiles/readme.org"}
  workspace-repo)

(defworkspace steam
  awesome-rules
  {:rules/apply
   (fn []
     (let [steam-client (awm/client-for-name "Steam")]
       (when steam-client
         (notify/notify
           "Found slack call client, moving to steam workspace"
           steam-client)
         (awm/ensure-tag "steam")
         (awm/move-client-to-tag (:awesome.client/window steam-client) "steam"))))})

(defworkspace audacity
  awesome-rules)

(defworkspace lichess
  awesome-rules
  {:workspace/exec "/usr/bin/gtk-launch firefox.desktop http://lichess.org"})

(defworkspace zoom
  {:awesome/rules (awm-workspace-rules "zoom" "Zoom" "Slack call")}
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
     (let [matches                             #{"zoom" "Zoom" "Slack call"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class)
           (string/includes? name "Slack call"))))}
  {:workspace/scratchpad         true
   :workspace/scratchpad-classes #{"zoom" "Slack"}
   :workspace/key                "z"})

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
   :workspace/exec               "/usr/bin/gtk-launch 1password.desktop"
   :workspace/key                "."})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org, mind-gardening, blogging, writing workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace journal
  awesome-rules
  {:workspace/directory        "Dropbox/todo"
   :workspace/initial-file     "journal.org"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"journal" "tauri/doctor-topbar"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  workspace-repo)

(defworkspace garden
  awesome-rules
  {:workspace/directory "Dropbox/todo/garden"}
  workspace-repo)

(defworkspace todo
  awesome-rules
  {:workspace/directory    "Dropbox/todo"
   :workspace/initial-file "projects.org"}
  workspace-repo)

(defworkspace blog
  awesome-rules
  {:workspace/directory "russmatney/blog-gatsby"}
  workspace-repo)

(defworkspace writing
  awesome-rules
  {:workspace/directory "/home/russ/Dropbox/Writing"}
  workspace-repo)

(defworkspace ink
  awesome-rules
  {:workspace/directory "/home/russ/Dropbox/todo/ink"}
  workspace-repo)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, Dotfiles, Emacs config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  awesome-rules
  {:git/check-status?   true
   :workspace/directory "russmatney/clawe"}
  workspace-repo)


(defworkspace ralphie
  awesome-rules
  {:git/check-status?   true
   :workspace/directory "russmatney/ralphie"}
  workspace-repo)

(defworkspace dotfiles
  awesome-rules
  {:git/check-status?   true
   :workspace/directory "russmatney/dotfiles"}
  workspace-repo)

(defworkspace emacs
  awesome-rules
  {:workspace/directory    ".doom.d"
   :workspace/initial-file "init.el"
   :git/check-status?      true}
  workspace-repo)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bindings, Workspaces, Util
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace bindings
  awesome-rules
  {:workspace/directory "russmatney/clawe"
   :workspace/files
   ["/home/russ/.doom.d/+bindings.el"
    "/home/russ/russmatney/clawe/src/clawe/defs/bindings.clj"
    "/home/russ/russmatney/clawe/awesome/bindings.fnl"]}
  workspace-repo)

(defworkspace workspaces
  awesome-rules
  {:workspace/directory "russmatney/clawe"
   :workspace/files     ["/home/russ/russmatney/clawe/src/clawe/defs/workspaces.clj"]}
  workspace-repo)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vapor, game repos, lovejs tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace vapor
  awesome-rules
  {:workspace/directory "russmatney/vapor"}
  workspace-repo)

(defworkspace platformer
  awesome-rules
  {:workspace/directory "russmatney/platformer"}
  workspace-repo)

(defworkspace beatemup
  awesome-rules
  {:workspace/directory "russmatney/beatemup"}
  workspace-repo)

(defworkspace lovejs
  awesome-rules
  {:workspace/directory "Davidobot/love.js"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc ~/russmatney/ repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doctor
  "A plasma app that records logs and reports misc statuses."
  awesome-rules
  {:workspace/directory "russmatney/doctor"
   :git/check-status?   true}
  workspace-repo)

(defworkspace scratch
  awesome-rules
  {:workspace/directory "russmatney/scratch"}
  workspace-repo)

(defworkspace yodo
  awesome-rules
  {:workspace/directory "russmatney/yodo-two"}
  workspace-repo)

(defworkspace yodo-dev
  awesome-rules
  {:workspace/directory "russmatney/yodo-two"}
  workspace-repo)

(defworkspace yodo-app
  awesome-rules
  {:workspace/exec "/usr/bin/gtk-launch google-chrome.desktop http://localhost:5600"})

(defworkspace doctor-todo
  awesome-rules
  {:workspace/directory       "russmatney/clawe"
   :workspace/exec            {:tmux/fire         "bb --config /home/russ/russmatney/clawe/bb.edn todo"
                               :tmux/session-name "doctor-todo"
                               :tmux/window-name  "doctor-todo"}
   :workspace/initial-file    "bb.edn"
   :workspace/scratchpad      true
   :workspace/scratchpad-name "tauri/doctor-todo"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"tauri/doctor-todo"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace doctor-popup
  awesome-rules
  {:workspace/directory       "russmatney/clawe"
   :workspace/exec            {:tmux/fire         "bb --config /home/russ/russmatney/clawe/bb.edn popup"
                               :tmux/session-name "doctor-popup"
                               :tmux/window-name  "doctor-popup"}
   :workspace/initial-file    "bb.edn"
   :workspace/scratchpad      true
   :workspace/scratchpad-name "tauri/doctor-popup"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"tauri/doctor-popup"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(comment
  (tmux/fire (:workspace/exec doctor-popup))
  )

(defworkspace org-crud
  awesome-rules
  {:workspace/directory "russmatney/org-crud"
   :git/check-status?   true}
  workspace-repo)

(defworkspace ink-mode
  awesome-rules
  {:workspace/directory "russmatney/ink-mode"}
  workspace-repo)

(defworkspace advent
  {:workspace/directory "russmatney/advent-of-code-2020"}
  workspace-repo
  awesome-rules)

(defworkspace chess
  {:workspace/directory "russmatney/chess"
   :git/check-status?   true}
  workspace-repo
  awesome-rules)

(defworkspace nix-pills
  {:workspace/directory "russmatney/nix-pills"}
  workspace-repo
  awesome-rules)

(defworkspace spotty
  {:workspace/directory "russmatney/spotty"}
  workspace-repo
  awesome-rules)

(defworkspace clover
  {:workspace/directory "russmatney/clover"}
  workspace-repo
  awesome-rules
  {:git/check-status? true})

(defworkspace expo
  awesome-rules
  {:workspace/directory "russmatney/expo"}
  workspace-repo
  {:git/check-status? true})

(defworkspace starters
  {:workspace/directory "russmatney/starters"}
  workspace-repo
  awesome-rules)

(defworkspace company-css-classes
  {:workspace/directory "russmatney/company-css-classes"}
  workspace-repo
  awesome-rules)

(defworkspace bb-task-completion
  {:workspace/directory "russmatney/bb-task-completion"}
  workspace-repo
  awesome-rules)

(defworkspace defthing
  awesome-rules
  {:workspace/directory "russmatney/defthing"}
  workspace-repo
  {:git/check-status? true})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Teknql repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace wing
  awesome-rules
  {:workspace/directory "teknql/wing"}
  workspace-repo
  {:git/check-status? true})

(defworkspace systemic
  awesome-rules
  {:workspace/directory "teknql/systemic"}
  workspace-repo
  {:git/check-status? true})

(defworkspace plasma
  awesome-rules
  {:workspace/directory "teknql/plasma"}
  workspace-repo
  {:git/check-status? true})

(defworkspace statik
  awesome-rules
  {:workspace/directory "teknql/statik"}
  workspace-repo)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Borkdude repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace sci
  {:workspace/directory "borkdude/sci"}
  workspace-repo
  awesome-rules)

(defworkspace carve
  {:workspace/directory "borkdude/carve"}
  workspace-repo
  awesome-rules)

(defworkspace clj-kondo
  {:workspace/directory "borkdude/clj-kondo"}
  workspace-repo
  awesome-rules)

(defworkspace babashka
  {:workspace/directory "borkdude/babashka"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace datalevin
  {:workspace/directory "juji-io/datalevin"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Urbint repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace grid
  awesome-rules
  {:workspace/directory "urbint/grid"
   :workspace/readme    "README.md"}
  workspace-repo
  {:git/check-status? true})

(defworkspace urbint
  awesome-rules
  {:workspace/directory "urbint/grid"
   :workspace/readme    "README.md"}
  workspace-repo)

(defworkspace lens
  awesome-rules
  {:workspace/directory "urbint/lens"
   :workspace/readme    "README.md"}
  workspace-repo
  {:git/check-status? true})

(defworkspace gitops
  {:workspace/directory "urbint/gitops"
   :workspace/readme    "README.md"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emacs repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doom-emacs
  awesome-rules
  {:workspace/color        "#aaee88"
   :workspace/directory    ".emacs.d"
   :workspace/initial-file "docs/index.org"}
  workspace-repo)

(defworkspace treemacs
  {:workspace/directory "Alexander-Miller/treemacs"}
  workspace-repo
  awesome-rules)

(defworkspace git-summary
  {:workspace/directory "MirkoLedda/git-summary"}
  workspace-repo
  awesome-rules)

(defworkspace clomacs
  {:workspace/directory "clojure-emacs/clomacs"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org roam dev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace org-roam
  {:workspace/directory "org-roam/org-roam"}
  workspace-repo
  awesome-rules)

(defworkspace org-roam-server
  {:workspace/directory "org-roam/org-roam-server"}
  workspace-repo
  awesome-rules)

(defworkspace md-roam
  {:workspace/directory "nobiot/md-roam"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lua Repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace awesomewm
  {:workspace/directory "awesomeWM/awesome"
   :workspace/readme    "README.md"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace aseprite
  {:workspace/directory "aseprite/aseprite"}
  workspace-repo
  awesome-rules)

;; TODO support creating a scratchpad workspace and keybinding completely from here
;; TODO support conflict alerts on keybindings via clj kondo helpers/the db? in-memory clj structures?
;; (defworkspace godot
;;   awesome-rules
;;   ;; {:rules/apply (fn []
;;   ;;                 ;; sometimes conflicts with the godot emacs/term sessions
;;   ;;                 ;; TODO this grabs browsers with 'godot' window titles
;;   ;;                 (let [clients (awm/clients-for-name "godot")]
;;   ;;                   (when (seq clients)
;;   ;;                     (awm/ensure-tag "godot")
;;   ;;                     (doall
;;   ;;                       (for [client clients]
;;   ;;                         (awm/move-client-to-tag (:awesome.client/window client) "godot"))))))}
;;   {:workspace/directory  "godot"
;;    :workspace/scratchpad true}
;;   workspace-repo)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Camsbury
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this should be generated by my ralphie-clone command
(defworkspace camsbury-config
  {:workspace/directory "Camsbury/config"}
  workspace-repo
  awesome-rules)

(defworkspace camsbury-xndr
  {:workspace/directory "Camsbury/xndr"}
  workspace-repo
  awesome-rules)

(defworkspace camsbury-bobby
  {:workspace/directory "Camsbury/bobby"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; abo-abo
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace abo-abo-hydra
  "A tool for chaining key presses in emacs."
  {:workspace/directory "abo-abo/hydra"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; baskerville
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace baskerville-sxhkd
  "A keybinding daemon."
  {:workspace/directory "baskerville/sxhkd"}
  workspace-repo
  awesome-rules)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; apache
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace apache-superset
  {:workspace/directory "apache/superset"}
  workspace-repo
  awesome-rules)
