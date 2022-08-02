(ns user
  (:require
   [defthing.defworkspace :as defworkspace]
   [defthing.defkbd :as defkbd]
   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]
   [wing.repl :as repl]
   [ralphie.zsh :as zsh]))

(comment
  (repl/sync-libs!))

(comment
  "yo"


  (binding
      [*data-readers*
       (->
         *data-readers*
         (assoc `sh/expand #'ralphie.zsh/expand)
         (assoc `sh/expand-many #'ralphie.zsh/expand-many))]
    *data-readers*

    )

  #zsh/expand "~/todo"
  #zsh/expand-many "~/todo/{projects,journal}.org"
  #to/upper "hi there"
  #to/lower "HI THERE"


  (def test-nses ['ralphie.awesome-test
                  'defthing.core-test
                  'defthing.defcom-test])

  (doall
    (for [t (->> test-nses
                 )]
      (require t)))

  (->>
    test-nses
    (map meta)
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
