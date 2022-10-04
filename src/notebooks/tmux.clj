(ns notebooks.tmux
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-cache   true}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
::clerk/visibility {:result :show}

;; ### tmux sessions

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux}))

;; ### tmux panes

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux-panes}))
