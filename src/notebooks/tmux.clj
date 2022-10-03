(ns notebooks.tmux
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide}
   :nextjournal.clerk/no-cache   true}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]))

;; ### tmux sessions

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux}))

;; ### tmux panes

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux-panes}))
