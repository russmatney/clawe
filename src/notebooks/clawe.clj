^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.clawe
  {:toc true}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [clawe.wm :as wm]))

;; ## current workspace
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(->>
  (wm/current-workspace)
  (remove (comp #(if (coll? %) (not (seq %)) (nil? %)) second))
  (into {}))

;; ### clients
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :clients}))

;; ### clients-all
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :clients
             :all  true}))

;; ### workspaces
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :workspaces}))

;; ### tmux sessions

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux}))

;; ### tmux panes

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux-panes}))

;; # defs

;; ### client-defs
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (clawe.config/client-defs))

;; ### workspace-defs
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/table
  {::clerk/width :full}
  (vals (clawe.config/workspace-defs-with-titles)))
