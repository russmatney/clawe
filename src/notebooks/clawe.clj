(ns notebooks.clawe
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-cache   true}
  (:require
   [clawe.debug :as debug]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [clawe.wm :as wm]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

(def current-workspace
  (->>
    (wm/current-workspace)
    (remove (comp #(if (coll? %) (not (seq %)) (nil? %)) second))
    (into {})))

{::clerk/visibility {:result :show}}

;; ## current workspace

current-workspace

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

;; # defs

;; ### client-defs

(clerk/table
  {::clerk/width :full}
  (clawe.config/client-defs))

;; ### workspace-defs

(clerk/table
  {::clerk/width :full}
  (vals (clawe.config/workspace-defs-with-titles)))
