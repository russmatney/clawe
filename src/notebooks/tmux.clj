(ns notebooks.tmux
  {:nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])
#_(clerk/add-viewers! [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{::clerk/visibility {:result :show}}

;; ### tmux sessions

^::clerk/no-cache
(clerk/table
  (debug/ls {:type :tmux}))

;; ### tmux panes

(clerk/table
  (debug/ls {:type :tmux-panes}))
