(ns user
  (:require
   [wing.repl :as repl]
   [clawe.workspaces :as workspaces]
   [clawe.awesome :as awm]))

(comment
  (repl/sync-libs!))

(comment

  ;; refresh workspaces widget
  (workspaces/update-workspaces)
  (awm/reload-widgets)

  ;; reload widgets (re-runs connect_for_each_screen)
  (awm/awm-cli "require('bar'); return init_screen();")

  ;; hotswap modules
  (awm/awm-cli "lume.hotswap('theme');")
  (awm/awm-cli "lume.hotswap('bar');")
  (awm/awm-cli "return lume.hotswap('widgets.workspaces');")

  (awm/awm-cli "return view(screen);")
  (awm/awm-cli "return view(screen.count());")


  (awm/awm-cli "return view(screen.count());")

  )
