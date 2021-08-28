(ns clawe.defs.workspaces
  (:require
   [defthing.defworkspace :refer [defworkspace]]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
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

(defn local-repo
  ([] (local-repo nil nil))
  ([repo-path]
   (local-repo repo-path nil))
  ([repo-path readme-path]
   (fn [wsp]
     (let [{:workspace/keys [directory initial-file]} wsp
           repo-path                                  (or repo-path directory)
           readme-path                                (or readme-path initial-file "readme.org")]
       {:git/repo            repo-path
        :workspace/directory (str "/home/russ/" repo-path)
        :workspace/initial-file
        (str "/home/russ/" repo-path "/" readme-path)}))))

(defn workspace-repo
  [{:workspace/keys [directory initial-file]}]
  (local-repo directory initial-file))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify, Web, other app-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace spotify
  {:awesome/rules
   (awm-workspace-rules "spotify"  "Spotify" "Pavucontrol" "pavucontrol")}
  {:workspace/directory        "/home/russ/"
   :workspace/exec             "spotify"
   :workspace/initial-file     "/home/russ/.config/spicetify/config.ini"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Spotify"
   :workspace/fa-icon-code     "f1bc"
   :rules/is-my-client?
   (fn [c]
     (let [matches
           #{"spotify" "Spotify"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))}
  (local-repo))

(defworkspace slack
  {:awesome/rules
   (awm-workspace-rules "slack" "discord")}
  {:workspace/directory        "/home/russ/"
   :workspace/exec             "slack"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Slack"
   :workspace/fa-icon-code     "f198"
   :rules/is-my-client?
   (fn [c]
     ;; TODO ignore 'Slack call' clients
     (let [matches                             #{"slack" "Slack"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))})

(defworkspace web
  {:awesome/rules
   ;; TODO get other awm names from rules
   ;; TODO don't catch 'firefoxdeveloperedition' here
   (awm-workspace-rules "web" "firefox")}
  {:workspace/directory        "/home/russ/"
   :workspace/exec             "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "firefox"
   :workspace/fa-icon-code     "f269"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"firefox" "web"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))
   })

(defworkspace dev-browser
  {:awesome/rules
   (awm-workspace-rules "dev-browser" "chrome" "Chrome" "Firefox Developer Edition"
                        "firefoxdeveloperedition")}
  {:workspace/directory          "/home/russ/"
   :workspace/exec               "/usr/bin/gtk-launch firefox-developer-edition.desktop"
   :workspace/scratchpad         true
   :workspace/scratchpad-classes #{"firefoxdeveloperedition"}
   :workspace/fa-icon-code       "f268"}
  {:rules/is-my-client?
   (fn [c]
     (let [matches
           #{"dev-browser" ;; emacs match? ;; include title by default?
             "chrome" "Chrome"
             "Firefox Developer Edition" "firefoxdeveloperedition" }
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))
   :rules/apply
   (fn []
     (let [x (awm/client-for-class "firefoxdeveloperedition")]
       (when x
         (notify/notify
           "Found firefoxdeveloper client, moving to dev-browser workspace"
           x)
         ;; TODO create workspace if it doesn't exist
         (awm/ensure-tag "dev-browser")
         (awm/move-client-to-tag (:awesome.client/window x) "dev-browser"))))} )

(defworkspace obs
  {:workspace/fa-icon-code "f130"
   :workspace/directory    "/home/russ/russmatney/obs-recordings"})

(defworkspace pixels
  {:workspace/fa-icon-code       "f03e"
   :workspace/directory          "/home/russ/Dropbox/pixels"
   :workspace/initial-file       "/home/russ/Dropbox/pixels/readme.org"
   :workspace/scratchpad-classes #{"Aseprite"}
   :workspace/exec               "/usr/bin/gtk-launch aseprite"}
  {:rules/apply (fn []
                  (let [client (awm/client-for-name "Aseprite")]
                    (when client
                      (awm/ensure-tag "pixels")
                      (awm/move-client-to-tag (:awesome.client/window client) "pixels")))
                  )} )

(defworkspace tiles
  {:workspace/fa-icon-code "f03e"
   :workspace/directory    "/home/russ/Dropbox/tiles"
   :workspace/initial-file "/home/russ/Dropbox/tiles/readme.org"})

(defworkspace steam
  awesome-rules
  {:workspace/fa-icon-code "f03e"
   :rules/apply
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
   :workspace/key                "z"
   :workspace/fa-icon-code       "f03e"})

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
  {:workspace/directory        "/home/russ/Dropbox/todo"
   :workspace/initial-file     "/home/russ/Dropbox/todo/journal.org"
   :workspace/color            "#42afff"
   ;; TODO give workspaces their own highlight color
   ;; (not just orange for everything)
   ;; TODO need to make this easier to preview, or live-updating
   :workspace/fa-icon-code     "f044"
   :workspace/key              "u"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"
   :rules/is-my-client?
   (fn [c]
     (let [matches                             #{"journal" "clover/doctor-dock"}
           {:awesome.client/keys [name class]} c]
       (or (matches name) (matches class))))
   })

(defworkspace garden
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/todo/garden"
   :workspace/initial-file "/home/russ/Dropbox/todo/garden/readme.org"
   :workspace/fa-icon-code "f18c"
   :workspace/key          "g"
   ;; :workspace/scratchpad       true
   ;; :workspace/scratchpad-class "Emacs"
   })

(defworkspace todo
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/todo"
   :workspace/initial-file "/home/russ/Dropbox/todo/projects.org"
   :workspace/fa-icon-code "f044"})

(defworkspace blog
  awesome-rules
  {:workspace/directory    "/home/russ/russmatney/blog-gatsby"
   :workspace/initial-file "/home/russ/russmatney/blog-gatsby/readme.org"})

(defworkspace writing
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/Writing"
   :workspace/initial-file "/home/russ/Dropbox/Writing/readme.org"})

(defworkspace ink
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/todo/ink"
   :workspace/initial-file "/home/russ/Dropbox/todo/ink/readme.md"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, Dotfiles, Emacs config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  awesome-rules
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/color        "#88aadd"
   ;; :workspace/title-pango  "<span>THE CLAWWEEEEEEEEE</span>"
   :workspace/title-pango  "<span size=\"large\">THE CLAWE</span>"
   :workspace/title-hiccup [:div
                            [:h1
                             {:class ["city-blue-500"]}
                             "The Cl-(awe)"]
                            ]
   :git/check-status?      true})

(defworkspace ralphie
  awesome-rules
  (fn [_] (local-repo "russmatney/ralphie"))
  {:workspace/color       "#aa88ee"
   :workspace/title-pango "<span>The Ralphinator</span>"
   :git/check-status?     true})

(defworkspace dotfiles
  awesome-rules
  (fn [_] (local-repo "russmatney/dotfiles"))
  {:git/check-status? true})

(defworkspace emacs
  awesome-rules
  (fn [_] (local-repo ".doom.d"))
  {:workspace/directory    "/home/russ/.doom.d"
   :workspace/initial-file "/home/russ/.doom.d/init.el"
   :workspace/fa-icon-code "f1d1"
   :git/check-status?      true
   :workspace/title-pango  "<span>Emax</span>"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bindings, Workspaces, Util
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace bindings
  awesome-rules
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/directory   "/home/russ/russmatney/clawe"
   :workspace/files
   ["/home/russ/.doom.d/+bindings.el"
    "/home/russ/russmatney/clawe/src/clawe/defs/bindings.clj"
    "/home/russ/russmatney/clawe/awesome/bindings.fnl"]
   :workspace/title-pango "<span>KEYBINDINGS</span>"})

(defworkspace workspaces
  awesome-rules
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/directory   "/home/russ/russmatney/clawe"
   :workspace/files       "/home/russ/russmatney/clawe/src/clawe/defs/workspaces.clj"
   :workspace/title-pango "<span>WSPCS</span>"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vapor, game repos, lovejs tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace vapor
  awesome-rules
  (fn [_] (local-repo "russmatney/vapor")))

(defworkspace platformer
  awesome-rules
  (fn [_] (local-repo "russmatney/platformer")))

(defworkspace beatemup
  awesome-rules
  (fn [_] (local-repo "russmatney/beatemup")))

(defworkspace lovejs
  awesome-rules
  (fn [_] (local-repo "Davidobot/love.js")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc ~/russmatney/ repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doctor
  "A plasma app that records logs and reports misc statuses."
  awesome-rules
  (fn [_] (local-repo "russmatney/doctor"))
  {:git/check-status? true})

(defworkspace scratch
  awesome-rules
  (fn [_] (local-repo "russmatney/scratch")))

(defworkspace yodo
  awesome-rules
  (fn [_] (local-repo "russmatney/yodo-two")))

(defworkspace yodo-dev
  awesome-rules
  (fn [_] (local-repo "russmatney/yodo-two")))

(defworkspace yodo-app
  awesome-rules
  {:workspace/exec "/usr/bin/gtk-launch google-chrome.desktop http://localhost:5600"})

(defworkspace org-crud
  awesome-rules
  (fn [_] (local-repo "russmatney/org-crud"))
  {:git/check-status? true}
  )

(defworkspace ink-mode
  awesome-rules
  (fn [_] (local-repo "russmatney/ink-mode")))

(defworkspace advent
  awesome-rules
  (fn [_] (local-repo "russmatney/advent-of-code-2020")))

(defworkspace chess
  awesome-rules
  (fn [_] (local-repo "russmatney/chess"))
  {:git/check-status? true})

(defworkspace nix-pills
  awesome-rules
  (fn [_] (local-repo "russmatney/nix-pills")))

(defworkspace spotty
  awesome-rules
  (fn [_] (local-repo "russmatney/spotty")))

(defworkspace clover
  awesome-rules
  (fn [_] (local-repo "russmatney/clover"))
  {:git/check-status? true})

(defworkspace expo
  awesome-rules
  (fn [_] (local-repo "russmatney/expo"))
  {:git/check-status? true})

(defworkspace starters
  awesome-rules
  (fn [_] (local-repo "russmatney/starters")))

(defworkspace company-css-classes
  awesome-rules
  (fn [_] (local-repo "russmatney/company-css-classes")))

(defworkspace bb-task-completion
  awesome-rules
  (fn [_] (local-repo "russmatney/bb-task-completion")))

(defworkspace defthing
  awesome-rules
  (fn [_] (local-repo "russmatney/defthing"))
  {:git/check-status? true})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Teknql repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace wing
  awesome-rules
  (fn [_] (local-repo "teknql/wing"))
  {:git/check-status? true})

(defworkspace systemic
  awesome-rules
  (fn [_] (local-repo "teknql/systemic"))
  {:git/check-status? true})

(defworkspace plasma
  awesome-rules
  (fn [_] (local-repo "teknql/plasma"))
  {:git/check-status? true})

(defworkspace statik
  awesome-rules
  (fn [_] (local-repo "teknql/statik")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Borkdude repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace sci
  awesome-rules
  (fn [_] (local-repo "borkdude/sci")))

(defworkspace carve
  awesome-rules
  (fn [_] (local-repo "borkdude/carve")))

(defworkspace clj-kondo
  awesome-rules
  (fn [_] (local-repo "borkdude/clj-kondo")))

(defworkspace babashka
  awesome-rules
  (fn [_] (local-repo "borkdude/babashka")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace datalevin
  awesome-rules
  (fn [_] (local-repo "juji-io/datalevin")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Urbint repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace grid
  awesome-rules
  (fn [_] (local-repo "urbint/grid" "README.md"))
  {:git/check-status? true})

(defworkspace urbint
  awesome-rules
  (fn [_] (local-repo "urbint/grid" "README.md")))

(defworkspace lens
  awesome-rules
  (fn [_] (local-repo "urbint/lens" "README.md"))
  {:git/check-status? true})

(defworkspace gitops
  awesome-rules
  (fn [_] (local-repo "urbint/gitops" "README.md")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emacs repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doom-emacs
  awesome-rules
  {:workspace/color        "#aaee88"
   :workspace/directory    "/home/russ/.emacs.d"
   :workspace/initial-file "/home/russ/.emacs.d/docs/index.org"
   :workspace/fa-icon-code "f1d1"
   :workspace/title-pango  "<span>Doom Emacs</span>"})

(defworkspace treemacs
  awesome-rules
  (fn [_] (local-repo "Alexander-Miller/treemacs")))

(defworkspace git-summary
  awesome-rules
  (fn [_] (local-repo  "MirkoLedda/git-summary")))

(defworkspace clomacs
  awesome-rules
  (fn [_] (local-repo "clojure-emacs/clomacs")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org roam dev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace org-roam
  awesome-rules
  (fn [_] (local-repo "org-roam/org-roam")))

(defworkspace org-roam-server
  awesome-rules
  (fn [_] (local-repo "org-roam/org-roam-server")))

(defworkspace md-roam
  awesome-rules
  (fn [_] (local-repo "nobiot/md-roam")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lua Repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace awesomewm
  awesome-rules
  (fn [_] (local-repo "awesomeWM/awesome" "README.md")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace aseprite
  awesome-rules
  (fn [_] (local-repo "aseprite/aseprite")))

;; TODO support creating a scratchpad workspace and keybinding completely from here
;; TODO support conflict alerts on keybindings via clj kondo helpers/the db? in-memory clj structures?
(defworkspace godot
  awesome-rules
  ;; {:rules/apply (fn []
  ;;                 ;; sometimes conflicts with the godot emacs/term sessions
  ;;                 ;; TODO this grabs browsers with 'godot' window titles
  ;;                 (let [clients (awm/clients-for-name "godot")]
  ;;                   (when (seq clients)
  ;;                     (awm/ensure-tag "godot")
  ;;                     (doall
  ;;                       (for [client clients]
  ;;                         (awm/move-client-to-tag (:awesome.client/window client) "godot"))))))}
  {:workspace/directory    "/home/russ/godot"
   :workspace/initial-file "/home/russ/godot/readme.org"
   :workspace/fa-icon-code "f130"
   :workspace/color        "#42afff"
   :workspace/scratchpad   true})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Camsbury
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this should be generated by my ralphie-clone command
(defworkspace camsbury-config
  awesome-rules
  (fn [_] (local-repo "Camsbury/config")))

(defworkspace camsbury-xndr
  awesome-rules
  (fn [_] (local-repo "Camsbury/xndr")))

(defworkspace camsbury-bobby
  awesome-rules
  (fn [_] (local-repo "Camsbury/bobby")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; abo-abo
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace abo-abo-hydra
  "A tool for chaining key presses in emacs."
  awesome-rules
  (fn [_] (local-repo "abo-abo/hydra")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; baskerville
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace baskerville-sxhkd
  "A keybinding daemon."
  awesome-rules
  (fn [_] (local-repo "baskerville/sxhkd")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; apache
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace apache-superset
  awesome-rules
  (fn [_] (local-repo "apache/superset")))
