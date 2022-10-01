(ns notebooks.clawe
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide}
   :nextjournal.clerk/no-cache   true}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [clawe.wm :as wm]
   [notebooks.nav :as nav]))

;; ## current workspace
(->>
  (wm/current-workspace)
  (remove (comp #(if (coll? %) (not (seq %)) (nil? %)) second))
  (into {}))

;; ### clients

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :clients}))

;; ### clients-all

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :clients
             :all  true}))

;; ### workspaces

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :workspaces}))

;; ### tmux sessions

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux}))

;; ### tmux panes

(clerk/table
  {::clerk/width :full}
  (debug/ls {:type :tmux-panes}))

;; # defs

;; ### client-defs

(clerk/table
  {::clerk/width :full}
  (clawe.config/client-defs))

;; ### workspace-defs

(clerk/table
  {::clerk/width :full}
  (vals (clawe.config/workspace-defs-with-titles)))

^{:nextjournal.clerk/visibility {:result :show}}
(clerk/md
  (nav/notebook-links))
