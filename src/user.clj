(ns user
  (:require
   [defthing.defworkspace :as defworkspace]
   [defthing.defkbd :as defkbd]
   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]
   [wing.repl :as repl]
   [clawe.workspaces :as workspaces]))

(comment
  (repl/sync-libs!))

(comment
  "yo"

  (->>
    (workspaces/all-workspaces)
    (filter :awesome.tag/name)
    )

  (awm/fetch-tags)



  (defkbd/list-bindings)
  (defworkspace/list-workspaces)

  ;; reload widgets (re-runs connect_for_each_screen)
  (awm/awm-cli "require('bar'); return init_bar();")

  (awm/awm-cli "lume.hotswap('theme');")
  (awm/awm-cli "lume.hotswap('clawe');")
  ;; hotswap modules
  (awm/awm-cli "lume.hotswap('theme');")
  (awm/awm-cli "lume.hotswap('bar');")
  (awm/awm-cli "return lume.hotswap('widgets.workspaces');")

  (awm/awm-cli "return view(screen);")
  (awm/awm-cli "return view(screen.count());")


  (awm/awm-cli "return view(screen.count());")

  (notify/notify "basic notification")
  (notify/notify "notification" "with body")
  (notify/notify {:subject "notification"
                  :body    "with body"})
  (awm/awm-fnl
    '(do
       (local naughty (require :naughty))
       (naughty.notify
         {:title "My notif"
          ;; :position "bottom_middle"
          :text  (.. "some sub head: " "with info")})))

  (notify/notify "updated notification")

  )
