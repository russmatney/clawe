(local spawn (require "awful.spawn"))

(local update-wsp-cb "update_workspaces_widget")
(fn update-workspaces [cb-fname]
  "Passes a callback function name to the update-dirty-workspaces function,
which is called from the clojure side."
  (spawn.easy_async
   (.. "bash -c \"clawe update-workspaces\"" " " (or cb-fname update-wsp-cb))
   (fn [_res]
     ;; no-op
     nil)))

{: update-workspaces}
