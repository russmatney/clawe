(ns clawe.scratchpad
  (:require
   [clawe.workspaces.create :as wsp.create]
   [clawe.awesome :as awm]
   [ralphie.awesome :as r.awm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ontop-and-focused [client]
  ;; set all ontops false
  ;; set all floating false
  ;; set this client ontop true
  ;; set this client floating true
  ;; focus this client

  (awm/awm-cli
    {:parse? false
     :pp?    false}
    (str
      ;; set all ontops false
      "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
      "c.ontop = false; "
      "c.floating = false; "
      "end;"

      ;; set this client ontop true, and focus it
      "for c in awful.client.iterate(function (c) return c.window == "
      (-> client :window awm/->lua-arg)
      " end) do\n"
      "c.ontop = true; "
      "c.floating = true; "

      ;; center and resize
      ;; "local f = (awful.placement.scale + awful.placement.centered);"
      ;; "f(c, {honor_padding=true, honor_workarea=true, to_percent= 0.75})"

      ;; just center
      "local f = awful.placement.centered;"
      "f(c);"
      "_G.client.focus = c;"
      "end; ")))

(defn toggle-scratchpad
  ([wsp]
   (let [wsp-name (some wsp [:workspace/title])
         tag      (-> wsp :awesome/tag)
         client   (some-> tag :clients first)]
     (cond
       ;; "found selected tag, client for:" wsp-name
       (and tag client (:selected tag))
       (if (:ontop client)
         ;; TODO also set client ontop false ?
         (r.awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; "found unselected tag, client for:" wsp-name
       (and tag client (not (:selected tag)))
       (do
         (r.awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; tag exists, no client
       (and tag (not client))
       (wsp.create/create-client wsp)

       ;; tag does not exist, presumably no client either
       (not tag)
       (do
         (r.awm/create-tag! wsp-name)
         (r.awm/toggle-tag wsp-name)
         (wsp.create/create-client wsp))))))

(comment
  (println "hi")
  (toggle-scratchpad "journal")

  (toggle-scratchpad "notes")
  (toggle-scratchpad "web")
  )
