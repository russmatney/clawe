(ns clawe.defs.local.workspaces
  "Consider .gitignoring and syncing up/down with the db."
  (:require
   [clawe.defs.workspaces :as defs.wrk]
   [defthing.defworkspace :refer [defworkspace]]
   [babashka.process :as process]
   [ralphie.zsh :as r.zsh]))

(defworkspace visual-scripting
  "Exploring godot and its visual-scripting features."
  {:workspace/directory "godot/visual-scripting"}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace editor
  "For recalling a consistent emacs editor."
  "Can be used generically, for any project."
  defs.wrk/awesome-rules
  {:workspace/directory        "/home/russ"
   :workspace/initial-file     "/home/russ"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"})

(defworkspace tile-maps
  "Exploring godot and its 2D tilemap features"
  {:workspace/directory "godot/tile-maps"}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace beatemup-two
  "beatemup-one rebuilt in godot"
  {:workspace/directory "godot/beatemup-two"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace lil-shooter
  "small top-down shooter in godot"
  {:workspace/directory "godot/lil-shooter"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace top-down-3d-shooter
  "small top-down 3d shooter in godot"
  {:workspace/directory "godot/top-down-3d-shooter"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace defthing-godot-tool-scripts
  "Trying to work out a nicer api here"
  {:workspace/directory "russmatney/defthing-godot-tool-scripts"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace rock-paper-goats
  "3d godot exploration"
  {:workspace/directory "russmatney/rock-paper-goats"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace super-repo
  "monorepo for game ideas"
  {:workspace/directory "russmatney/super-repo"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(comment
  (def src (r.zsh/expand "~/godot/top-down-3d-shooter"))
  (def new (str (r.zsh/expand "~/") "/russmatney/rock-paper-goats"))
  (:workspace/directory rock-paper-goats)
  (:workspace/directory top-down-3d-shooter)

  (->
    (process/$ mkdir -p ~(:workspace/directory rock-paper-goats))
    (process/check))
  (->
    (process/$ cp -r ~(:workspace/directory top-down-3d-shooter) ~(str (:workspace/directory rock-paper-goats) "/."))
    (process/check))
  )

(defworkspace flatformer
  "2d godot platformer"
  {:workspace/directory "russmatney/flatformer"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace first-vania
  "2d godot metroid vania"
  {:workspace/directory "russmatney/first-vania"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace clojuregodottest
  {:workspace/directory "russmatney/clojuregodottest"}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace lifeofbob
  {:workspace/directory "russmatney/lifeofbob"
   :git/repo            "russmatney/lifeofbob"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace arcadia-godot
  {:workspace/directory "arcadia-unity/ArcadiaGodot"
   :git/repo            "arcadia-unity/ArcadiaGodot"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)


(defworkspace thecreeps-godotclj
  {:workspace/directory "tristanstraub/thecreeps-godotclj"
   :git/repo            "tristanstraub/thecreeps-godotclj"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace tristanstraub-godotclj
  {:workspace/directory "tristanstraub/godotclj"
   :git/repo            "tristanstraub/godotclj"}
  {:git/check-status? true}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)
