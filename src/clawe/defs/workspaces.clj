(ns clawe.defs.workspaces
  (:require
   [clawe.defthing :as defthing]
   [ralphie.awesome :as r.awm]
   [clojure.string :as string]
   [ralphie.notify :as notify]
   [ralph.defcom :refer [defcom]]
   [babashka.process :as process]
   [ralphie.tmux :as tmux]
   [clawe.awesome :as awm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-workspaces []
  (defthing/list-xs :clawe/workspaces))

(defn get-workspace [wsp]
  (defthing/get-x :clawe/workspaces
    (comp #{(if (map? wsp)
              (some wsp [:workspace/title :awesome/tag-name :name])
              wsp)}
          :name)))

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
  "Sets the workspace title using the :name (which defaults to the symbol).

  This only happens to work b/c defthing sets the {:name blah} attr with
  the passed symbol.
  "
  [{:keys [name]}]
  {:workspace/title name})

;; TODO rewrite to work on a {:workspace/directory ""} if it exists
(defn local-repo
  ([repo-path]
   (local-repo repo-path nil))
  ([repo-path readme-path]
   (let [readme-path (or readme-path "readme.org")]
     {:git/repo               repo-path
      :workspace/directory    (str "/home/russ/" repo-path)
      :workspace/initial-file
      (str "/home/russ/" repo-path "/" readme-path)})))

(defn workspace-repo
  [{:workspace/keys [directory initial-file]}]
  (local-repo directory initial-file))


(defmacro defworkspace [title & args]
  (apply defthing/defthing :clawe/workspaces title
         (conj args `workspace-title)))

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
  {:workspace/color            "#38b98a"
   :workspace/directory        "/home/russ/"
   :workspace/exec             "spotify"
   :workspace/initial-file     "/home/russ/.config/spicetify/config.ini"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Spotify"
   :workspace/key              "s"
   :workspace/fa-icon-code     "f1bc"})

(defworkspace slack
  workspace-title
  {:awesome/rules
   (awm-workspace-rules "slack" "discord")}
  {:workspace/color            "#38b98a"
   :workspace/directory        "/home/russ/"
   :workspace/exec             "slack"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Slack"
   :workspace/key              "a"
   :workspace/fa-icon-code     "f198"})

(defworkspace web
  workspace-title
  {:awesome/rules
   ;; TODO get other awm names from rules
   (awm-workspace-rules "web" "firefox" "Firefox")}
  {:workspace/color            "#38b98a"
   :workspace/directory        "/home/russ/"
   :workspace/exec             "/usr/bin/gtk-launch firefox.desktop"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "firefox"
   :workspace/key              "t"
   :workspace/fa-icon-code     "f269"})

(defworkspace chrome-browser
  workspace-title
  {:awesome/rules
   ;; TODO get other awm names from rules
   (awm-workspace-rules "chrome-browser" "chrome" "Chrome")}
  {:workspace/color            "#38b98a"
   :workspace/directory        "/home/russ/"
   :workspace/exec             "/usr/bin/gtk-launch google-chrome.desktop"
   :workspace/scratchpad       true
   :workspace/key              "b"
   :workspace/fa-icon-code     "f268"})

(defworkspace obs
  workspace-title
  {:workspace/fa-icon-code "f130"
   :workspace/directory    "/home/russ/russmatney/obs-recordings"})

(defworkspace pixels
  workspace-title
  {:workspace/fa-icon-code "f03e"
   :workspace/directory    "/home/russ/Dropbox/pixels"
   :workspace/initial-file "/home/russ/Dropbox/pixels/readme.org"}
  {:rules/apply (fn []
                  (let [client (awm/client-for-name "Aseprite")]
                    (when client
                      (awm/move-client-to-tag (:window client) "pixels"))))} )

(defworkspace tiles
  workspace-title
  {:workspace/fa-icon-code "f03e"
   :workspace/directory    "/home/russ/Dropbox/tiles"
   :workspace/initial-file "/home/russ/Dropbox/tiles/readme.org"})

(defworkspace steam
  workspace-title
  awesome-rules)

(defworkspace audacity
  awesome-rules
  workspace-title)

(defworkspace lichess
  awesome-rules
  workspace-title
  {:workspace/exec "/usr/bin/gtk-launch firefox.desktop http://lichess.org"})

(defworkspace zoom
  {:awesome/rules (awm-workspace-rules "zoom" "Zoom" "Slack call")}
  {:rules/apply (fn []
                  (let [slack-call (awm/client-for-name "Slack call")]
                    (when slack-call
                      (notify/notify
                        "Found slack call client, moving to zoom workspace"
                        slack-call)
                      (awm/move-client-to-tag (:window slack-call) "zoom"))))}
  workspace-title
  {:workspace/scratchpad   true
   ;; :workspace/scratchpad-class "Zoom"
   :workspace/key          "z"
   :workspace/fa-icon-code "f03e"})

(defworkspace one-password
  {:awesome/rules (awm-workspace-rules "1password" "1Password")}
  {:rules/apply (fn []
                  (let [c (awm/client-for-name "1Password")]
                    (when c
                      (notify/notify
                        "Found 1password client, moving to zoom workspace"
                        c)
                      (awm/move-client-to-tag (:window c) "one-password"))))}
  workspace-title
  {:workspace/scratchpad   true
   :workspace/exec         "/usr/bin/gtk-launch 1password.desktop"
   :workspace/key          "."})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org, mind-gardening, blogging, writing workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace journal
  workspace-title
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
   })

(defworkspace garden
  workspace-title
  awesome-rules
  {:workspace/directory        "/home/russ/Dropbox/todo/garden"
   :workspace/initial-file     "/home/russ/Dropbox/todo/garden/readme.org"
   :workspace/fa-icon-code     "f18c"
   :workspace/key              "g"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"
   })

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
   :workspace/title-hiccup [:div
                            [:h1
                             {:class ["city-blue-500"]}
                             "The Cl-(awe)"]
                            ]
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
  workspace-title
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/directory    "/home/russ/russmatney/clawe"
   :workspace/initial-files
   ["/home/russ/.doom.d/+bindings.el"
    "/home/russ/russmatney/clawe/src/clawe/defs/bindings.clj"
    "/home/russ/russmatney/clawe/awesome/bindings.fnl"]
   :workspace/title-pango  "<span>KEYBINDINGS</span>"})

(defworkspace workspaces
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/clawe"))
  {:workspace/directory    "/home/russ/russmatney/clawe"
   :workspace/initial-files "/home/russ/russmatney/clawe/src/clawe/defs/workspaces.clj"
   :workspace/title-pango  "<span>WSPCS</span>"})

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

(defcom toggle-doctor-dock
  {:defcom/name "toggle-doctor-dock"
   :defcom/handler
   (fn [_ _]
     (notify/notify "toggling-doctor-dock")
     (println "toggling-doctor-dock")

     (let [doctor-server-started? true]
       ;; TODO start doctor server if not started
       ;; TODO toggle dock into view if hidden
       ;; TODO toggle dock out of view if visible
       ;; might reference the emacs tmux fire command for vapor/love2d restarts
       ;; in my dotfiles

       (tmux/fire "yrun dev http://localhost:5555 doctor"
         {:tmux/directory "/home/russ/russmatney/clover"
          :tmux/window-name  "clover-doctor"
          :tmux/session-name "doctor"})))})

(comment
  (toggle-doctor-dock nil nil)
  )

(defworkspace doctor
  "A plasma app that records logs and reports misc statuses."
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/doctor"))
  {:git/check-status? true})

(defworkspace scratch
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/scratch")))

(defworkspace yodo
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/yodo-two")))

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
  (fn [_] (local-repo "russmatney/org-crud"))
  {:git/check-status? true}
  )

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
  (fn [_] (local-repo "russmatney/chess"))
  {:git/check-status? true})

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

(defworkspace expo
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/expo")))

(defworkspace starters
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/starters")))

(defworkspace company-css-classes
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/company-css-classes")))

(defworkspace bb-task-completion
  awesome-rules
  workspace-title
  (fn [_] (local-repo "russmatney/bb-task-completion")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Teknql repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace wing
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/wing"))
  {:git/check-status? true})

(defworkspace systemic
  awesome-rules
  workspace-title
  (fn [_] (local-repo "teknql/systemic"))
  {:git/check-status? true})

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
;; Clojure repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace datalevin
  awesome-rules
  workspace-title
  (fn [_] (local-repo "juji-io/datalevin")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Urbint repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace grid
  awesome-rules
  workspace-title
  (fn [_] (local-repo "urbint/grid" "README.md"))
  {:git/check-status? true})

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
  {:rules/apply (fn []
                  ;; sometimes conflicts with the godot emacs/term sessions
                  (let [clients (awm/clients-for-name "godot")]
                    (when (seq clients)
                      (doall
                        (for [client clients]
                          (awm/move-client-to-tag (:window client) "godot"))))))}
  {:workspace/directory "/home/russ/godot"
   :workspace/initial-file "/home/russ/godot/readme.org"
   :workspace/fa-icon-code "f130"
   :workspace/color "#42afff"
   :workspace/scratchpad true})
