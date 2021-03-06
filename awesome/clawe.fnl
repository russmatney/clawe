(local spawn (require "awful.spawn"))
(local view (require "fennelview"))

(local update-wsp-cb "update_workspaces_widget")
(fn update-workspaces [cb-fname]
  "Passes a callback function name to the update-dirty-workspaces function,
which is called from the clojure side."
  (spawn.easy_async
   (.. "bash -c \"clawe update-workspaces\"" " " (or cb-fname update-wsp-cb))
   (fn [_res]
     ;; no-op
     nil)))

(fn cmd [cmd-name]
  (spawn.easy_async
   (.. "bash -c \"clawe " cmd-name "\"")
   (fn [_res]
     ;; no-op
     nil)))

(fn cmd-args [cmd-name arg]
  (let [s (.. "bash -c \"clawe " cmd-name " '" (view arg) "'\"")]
    (spawn.easy_async s (fn [_res] nil))))

{: update-workspaces
 : cmd
 : cmd-args}
