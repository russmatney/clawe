^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.clawe
  (:require [clawe.debug :as debug]
            [nextjournal.clerk :as clerk]
            [clawe.config :as clawe.config]
            [clawe.wm :as wm]
            [babashka.fs :as fs]))

^{:nextjournal.clerk/visibility {:code :hide}}
(defn rerender []
  ;; TODO better way to get *file* when called from elsewhere
  ;; maybe a macro?
  (clerk/show! (str (fs/home) "/russmatney/clawe/src/notebooks/clawe.clj")))

;; ### current workspace
^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(wm/current-workspace)

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
