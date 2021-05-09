(ns clawe.defs.local.workspaces
  "Consider .gitignoring and syncing up/down with the db."
  (:require
   [clawe.defs.workspaces :as defs.wrk :refer [defworkspace]]))

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
   :workspace/color            "#af42ff"
   :workspace/fa-icon-code     "f044"
   :workspace/key              "e"
   :workspace/scratchpad       true
   :workspace/scratchpad-class "Emacs"})

(defworkspace tile-maps
  "Exploring godot and its 2D tilemap features"
  {:workspace/directory "godot/tile-maps"}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)

(defworkspace beatemup-two
  "beatemup rebuilt in godot"
  {:workspace/directory "godot/beatemup-two"}
  defs.wrk/awesome-rules
  defs.wrk/workspace-repo)
