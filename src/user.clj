(ns user
  (:require
   [wing.repl :as repl]
   [clawe.workspaces :as workspaces]
   [clawe.awesome :as awm]
   [clawe.defs.bindings :as bindings]
   [clawe.defs.workspaces :as defs.wsp]
   [ralphie.notify :as notify]))

(comment
  (repl/sync-libs!))

(comment
  (bindings/list-bindings)
  (defs.wsp/list-workspaces)
  (workspaces/active-workspaces)
  (workspaces/update-workspaces)
  (awm/reload)

  ;; reload widgets (re-runs connect_for_each_screen)
  (awm/awm-cli "require('bar'); return init_bar();")

  (awm/awm-cli "lume.hotswap('widgets.org-clock');")
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
                  :body "with body"})
  (awm/awm-fnl
    '(do
       (local naughty (require :naughty))
       (naughty.notify
           {:title "My notif"
            ;; :position "bottom_middle"
            :text (.. "some sub head: " "with info")})))

  (notify/notify "updated notification")

  )
