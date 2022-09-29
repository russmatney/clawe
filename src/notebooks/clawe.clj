^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.clawe
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [clawe.wm :as wm]
   [notebooks.nav :as nav]))

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer     nav/nav-viewer}
nav/nav-options

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
