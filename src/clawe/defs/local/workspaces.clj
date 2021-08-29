(ns clawe.defs.local.workspaces
  "Consider .gitignoring and syncing up/down with the db."
  (:require
   [clawe.defs.workspaces :as defs.wrk]
   [defthing.defworkspace :refer [defworkspace]]))

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
