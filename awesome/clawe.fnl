(local spawn (require "awful.spawn"))

(fn update-workspaces [cb-fname]
  "Passes a callback function name to the update-dirty-workspaces function,
which is called from the clojure side."
  (spawn.easy_async
   (.. "bash -c \"clawe update-workspaces\""
       " "
       cb-fname)
   #(print (.. "workspaces update requested with cb-fname: " cb-fname))))

{: update-workspaces}
