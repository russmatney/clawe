^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.clawe
  (:require [clawe.debug :as debug]
            [nextjournal.clerk :as clerk]
            [clawe.config :as clawe.config]
            [clawe.wm :as wm]))

;; ### current workspace
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(->>
  (wm/current-workspace)
  (remove (comp #(if (coll? %) (not (seq %)) (nil? %)) second))
  (into {}))

;; ## client-defs
^{::clerk/visibility {:code :hide}}
(clerk/table (clawe.config/client-defs))

;; ## clients
^{::clerk/visibility {:code :hide}}
(clerk/table (debug/ls {:type :clients}))

;; ## workspace-defs
^{::clerk/visibility {:code :hide}}
(clerk/table (vals (clawe.config/workspace-defs-with-titles)))

;; ## workspaces
^{::clerk/visibility {:code :hide}}
(clerk/table (debug/ls {:type :workspaces}))
