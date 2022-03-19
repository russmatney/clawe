(ns clawe.defs.workspaces
  (:require
   [clojure.string :as string]

   [defthing.defworkspace :as defworkspace :refer [defworkspace]]
   [defthing.core :as defthing]

   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]))

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
;; Dynamic workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->repo-desc [path]
  (let [reversed (-> path (string/split #"/") reverse)]
    {:repo/name      (first reversed)
     :repo/user-name (second reversed)
     :repo/path      path}))

(defn git-user->repo-descs
  "Expects a user or org (a repo prefix).
  Expands `~/<user>/*` via zsh and describes repos in there,
  via `path->repo-desc`

  (git-user->repo-descs \"russmatney\")

  => { :repo/name \"bb-cli\", :repo/user-name \"russmatney\", :repo/path \"/home/russ/russmatney/bb-cli\" }
  ...etc.
  "
  ([user] (git-user->repo-descs user nil))
  ([user repos]
   (->> (zsh/expand-many (str "~/" user "/*"))
        (map path->repo-desc)
        ((fn [xs]
           (if repos
             (filter (comp repos :repo/name) xs)
             xs))))))

;; :git/check-status?   true
;; TODO minimal way to opt-in to expensive git status checks?
;;  maybe db toggles via rofi
;;  or a git dashboard across all these

;; TODO rewrite to be less crazy
(defn repo-desc->workspace
  [{:repo/keys [name user-name _path] :as desc}]
  (if (and name user-name)
    (-> desc
        ((fn [x]
           (merge x
                  (defthing/initial-thing :clawe/workspaces name))))
        ((fn [x]
           (merge x
                  (defworkspace/workspace-title x))))
        ((fn [x]
           (merge x
                  {:workspace/directory (str user-name "/" name)
                   :workspace/readme    "README.md"})))
        ((fn [x]
           (merge x
                  (workspace-repo x)))))
    (do
      (println "Missing name or user-name" desc)
      ;; (throw Exception)
      )))

(comment
  (git-user->repo-descs "teknql")
  (->
    (git-user->repo-descs "teknql")
    first
    repo-desc->workspace))


(defn build-workspaces-for-git-user
  ([user] (build-workspaces-for-git-user user nil))
  ([user repos]
   (->>
     (git-user->repo-descs user repos)
     (map repo-desc->workspace)
     (remove nil?))))

(comment
  (->>
    (defworkspace/list-workspaces)
    (filter (comp (fnil #(string/includes? % "urbint") "") :repo/user-name))
    )

  (build-workspaces-for-git-user "urbint")
  (build-workspaces-for-git-user "teknql")
  )

(defn load-workspaces
  "Loads current workspace state into the clawe db, from whence it shall be read.

  Expects a list of git user names or vectors with a set of repos to filter by.

  Ex: (load-workspaces [\"teknql\" [\"russmatney\" #{\"dotfiles\" \"clawe\"}]])
  This will load all `~/teknql/*` repos, `~/russmatney/dotfiles`, `~/russmatney/clawe`."
  [repo-users-and-names]
  (notify/notify "loading-workspaces" repo-users-and-names)

  (->> repo-users-and-names
       (map (fn [arg]
              (cond
                (string? arg)
                (build-workspaces-for-git-user arg)
                (vector? arg)
                (apply build-workspaces-for-git-user arg))))
       flatten
       (map defthing/add-thing)
       (map #(notify/notify "loaded" (:repo/path %)))))

(comment
  (load-workspaces ["teknql"])

  (load-workspaces [["russmatney" #{"dotfiles"}]])

  (load-workspaces
    ;; maybe this comes from a config.edn? or deps.edn?
    ;; or just set in the db/via rofi?
    [["russmatney" #{"dotfiles" "protomoon"}]
     "teknql"
     ["urbint" #{"grid" "lens" "gitops"
                 "worker-safety-service"
                 "worker-safety-client"}]
     "borkdude"])

  (load-workspaces
    [["urbint" #{"grid" "lens" "gitops"
                 "worker-safety-service"
                 "worker-safety-client"}]])

  (->>
    (defworkspace/list-workspaces)
    (filter (comp (fnil #(string/includes? % "urbint") "") :repo/user-name))))

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
                      (awm/move-client-to-tag (:awesome.client/window client) "pixels"))))}
  workspace-repo)

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
  {:workspace/directory "Dropbox/todo/garden"}
  workspace-repo)

(defworkspace ink
  {:workspace/directory "/home/russ/Dropbox/todo/ink"}
  workspace-repo)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Doctor apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace doctor-todo
  {:workspace/directory       "Dropbox/todo"
   :workspace/exec            {:tmux/fire         "bb --config /home/russ/russmatney/clawe/bb.edn todo"
                               :tmux/session-name "doctor-todo"
                               :tmux/interrupt?   true}
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
   :workspace/exec            {:tmux/fire         "bb --config /home/russ/russmatney/clawe/bb.edn popup"
                               :tmux/session-name "doctor-popup"
                               :tmux/interrupt?   true}
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emacs repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace emacs
  {:workspace/directory    ".doom.d"
   :workspace/initial-file "init.el"
   :git/check-status?      true}
  workspace-repo)

(defworkspace doom-emacs
  {:workspace/color        "#aaee88"
   :workspace/directory    ".emacs.d"
   :workspace/initial-file "docs/index.org"}
  workspace-repo)
