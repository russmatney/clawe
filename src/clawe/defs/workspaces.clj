(ns clawe.defs.workspaces
  (:require
   [clawe.defthing :as defthing]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-workspaces []
  (defthing/list-xs :clawe/workspaces))

(defn get-workspace [wsp]
  (defthing/get-x :clawe/workspaces
    (comp #{(some wsp [:workspace/title :awesome/tag-name :name identity])}
          :name)))

(defmacro defworkspace [title & args]
  (apply defthing/defthing :clawe/workspaces title args))

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

(defn workspace-title
  "Sets the workspace title using the :name (which defaults to the symbol)."
  [{:keys [name]}]
  {:workspace/title name})

(defn local-repo
  ([repo-path]
   (local-repo repo-path
               ;; TODO auto-discover readmes
               "readme.org"))
  ([repo-path readme-path]
   {:git/repo               repo-path
    :workspace/directory    (str "/home/russ/" repo-path)
    :workspace/initial-file
    (str "/home/russ/" repo-path "/" readme-path)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; usage examples
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (defworkspace my-workspace
    workspace-title
    awesome-rules))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify, Web, other app-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace spotify
  workspace-title
  {:awesome/rules
   (awm-workspace-rules "spotify"  "spotify" "Pavucontrol" "pavucontrol")}
  {:workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/"
   :workspace/exec         "spotify"
   :workspace/initial-file "/home/russ/.config/spicetify/config.ini"
   :workspace/scratchpad   true
   :workspace/key          "s"
   :workspace/fa-icon-code "f1bc"})

(defworkspace slack
  workspace-title
  {:awesome/rules
   (awm-workspace-rules "slack" "discord")}
  {:workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/"
   :workspace/exec         "slack"
   :workspace/scratchpad   true
   :workspace/key          "a"
   :workspace/fa-icon-code "f198"})

(defworkspace web
  workspace-title
  {:awesome/rules
   ;; TODO get other awm names from rules
   (awm-workspace-rules "web" "firefox" "Firefox" "chrome")}
  {:workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/"
   :workspace/exec         "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad   true
   :workspace/key          "t"
   :workspace/fa-icon-code "f269"})

(defworkspace obs
  workspace-title
  {:workspace/fa-icon-code "f130"
   :workspace/directory    "/home/russ/russmatney/obs-recordings"})

(defworkspace pixels
  workspace-title
  {:workspace/fa-icon-code "f03e"
   :workspace/directory    "/home/russ/Dropbox/pixels"
   :workspace/initial-file "/home/russ/Dropbox/pixels/readme.org"})

(defworkspace tiles
  workspace-title
  {:workspace/fa-icon-code "f03e"
   :workspace/directory    "/home/russ/Dropbox/tiles"
   :workspace/initial-file "/home/russ/Dropbox/tiles/readme.org"})

(defworkspace steam
  awesome-rules
  workspace-title)

(defworkspace audacity
  awesome-rules
  workspace-title)

(defworkspace lichess
  awesome-rules
  workspace-title
  {:workspace/exec "/usr/bin/gtk-launch firefox.desktop http://lichess.org"})

(defworkspace zoom
  {:awesome/rules (awm-workspace-rules "zoom" "Zoom")}
  workspace-title
  {:workspace/scratchpad true
   :workspace/key        "z"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org, mind-gardening, blogging, writing workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace journal
  workspace-title
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/todo"
   :workspace/initial-file "/home/russ/Dropbox/todo/journal.org"
   :workspace/fa-icon-code "f044"
   :workspace/key          "u"
   :workspace/scratchpad   true})

(defworkspace garden
  workspace-title
  awesome-rules
  {:workspace/directory "/home/russ/Dropbox/todo/garden"
   :workspace/initial-file "/home/russ/Dropbox/todo/garden/readme.org"
   :workspace/fa-icon-code "f18c"
   :workspace/key          "g"
   :workspace/scratchpad   true})

(defworkspace todo
  workspace-title
  awesome-rules
  {:workspace/directory "/home/russ/Dropbox/todo"
   :workspace/initial-file "/home/russ/Dropbox/todo/projects.org"
   :workspace/fa-icon-code "f044"})

(defworkspace blog
  workspace-title
  awesome-rules
  {:workspace/directory    "/home/russ/russmatney/blog-gatsby"
   :workspace/initial-file "/home/russ/russmatney/blog-gatsby/readme.org"})

(defworkspace writing
  workspace-title
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/Writing"
   :workspace/initial-file "/home/russ/Dropbox/Writing/readme.org"})

(defworkspace ink
  workspace-title
  awesome-rules
  {:workspace/directory    "/home/russ/Dropbox/todo/ink"
   :workspace/initial-file "/home/russ/Dropbox/todo/ink/readme.md"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, Dotfiles, Emacs config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  workspace-title
  awesome-rules
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/color        "#88aadd"
   ;; :workspace/title-pango  "<span>THE CLAWWEEEEEEEEE</span>"
   :workspace/title-pango  "<span size=\"large\">THE CLAWE</span>"
   :workspace/title-hiccup [:h1 "The Cl-(awe)"]
   :git/check-status?      true})

(defworkspace ralphie
  workspace-title
  awesome-rules
  (fn [_] (local-repo "russmatney/ralphie"))
  {:workspace/color       "#aa88ee"
   :workspace/title-pango "<span>The Ralphinator</span>"
   :git/check-status?     true})

(defworkspace dotfiles
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/dotfiles"))
  {:git/check-status? true})

(defworkspace emacs
  awesome-rules
  workspace-title
  {:workspace/directory    "/home/russ/.doom.d"
   :workspace/initial-file "/home/russ/.doom.d/init.el"
   :workspace/fa-icon-code "f1d1"
   :git/check-status?      true
   :workspace/title-pango  "<span>Emax</span>"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vapor, game repos, lovejs tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace vapor
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/vapor")))

(defworkspace platformer
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/platformer")))

(defworkspace beatemup
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/beatemup")))

(defworkspace lovejs
  awesome-rules
  workspace-title
  (fn [_] (local-repo "Davidobot/love.js")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc ~/russmatney/ repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace scratch
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/scratch")))

(defworkspace yodo-dev
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/yodo-two")))

(defworkspace yodo-app
  awesome-rules
  workspace-title
  {:workspace/exec "/usr/bin/gtk-launch google-chrome.desktop http://localhost:5600"})

(defworkspace org-crud
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/org-crud")))

(defworkspace ink-mode
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/ink-mode")))

(defworkspace advent
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/advent-of-code-2020")))

(defworkspace chess
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/chess")))

(defworkspace nix-pills
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/nix-pills")))

(defworkspace spotty
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/spotty")))

(defworkspace clover
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/clover")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Teknql repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace wing
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/wing")))

(defworkspace systemic
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/systemic")))

(defworkspace plasma
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/plasma")))

(defworkspace statik
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/statik")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Borkdude repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace sci
  awesome-rules
  workspace-title
  (fn [_] (local-repo "borkdude/sci")))

(defworkspace carve
  awesome-rules
  workspace-title
  (fn [_] (local-repo "borkdude/carve")))

(defworkspace clj-kondo
  awesome-rules
  workspace-title
  (fn [_] (local-repo "borkdude/clj-kondo")))

(defworkspace babashka
  awesome-rules
  workspace-title
  (fn [_] (local-repo "borkdude/babashka")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Urbint repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace grid
  awesome-rules
  workspace-title
  (fn [_] (local-repo "urbint/grid")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emacs repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doom-emacs
  workspace-title
  awesome-rules
  {:workspace/color        "#aaee88"
   :workspace/directory    "/home/russ/.emacs.d"
   :workspace/initial-file "/home/russ/.emacs.d/docs/index.org"
   :workspace/fa-icon-code "f1d1"
   :workspace/title-pango  "<span>Doom Emacs</span>"})

(defworkspace treemacs
  awesome-rules
  workspace-title
  (fn [_] (local-repo "Alexander-Miller/treemacs")))

(defworkspace git-summary
  awesome-rules
  workspace-title
  (fn [_] (local-repo  "MirkoLedda/git-summary")))

(defworkspace clomacs
  awesome-rules
  workspace-title
  (fn [_] (local-repo "clojure-emacs/clomacs")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org roam dev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace org-roam
  awesome-rules
  workspace-title
  (fn [_] (local-repo "org-roam/org-roam")))

(defworkspace org-roam-server
  awesome-rules
  workspace-title
  (fn [_] (local-repo "org-roam/org-roam-server")))

(defworkspace md-roam
  awesome-rules
  workspace-title
  (fn [_] (local-repo "nobiot/md-roam")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lua Repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace awesomewm
  awesome-rules
  workspace-title
  (fn [_] (local-repo "awesomeWM/awesome" "README.md")))