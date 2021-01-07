(ns user
  (:require
   [wing.repl :as repl]
   [clawe.awesome :as awm]))

(comment
  (repl/sync-libs!))

(comment

  (awm/reload-widgets)

  ;; reload widgets (re-runs connect_for_each_screen)
  (awm/awm-cli "require('bar'); init_screen();")

  ;; hotswap modules
  (awm/awm-cli "lume.hotswap('bar');")
  (awm/awm-cli "lume.hotswap('widgets.workspaces');")

  (awm/awm-cli "return view(screen);")
  (awm/awm-cli "return view(screen.count());")

  )
