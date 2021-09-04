(local spawn (require "awful.spawn"))
(local view (require "fennelview"))

(fn cmd [cmd-name]
  (spawn.easy_async
   (.. "bash -c \"clawe " cmd-name "\"")
   (fn [_res]
     ;; no-op
     nil)))

(fn cmd-args [cmd-name arg]
  (let [s (.. "bash -c \"clawe " cmd-name " '" (view arg) "'\"")]
    (spawn.easy_async s (fn [_res] nil))))

{: cmd
 : cmd-args}
