(ns clawe.scratchpad
  (:require
   [clawe.workspaces.create :as wsp.create]
   [clawe.awesome :as awm]
   [ralphie.awesome :as r.awm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
         (awm/focus-client client))

       ;; "found unselected tag, client for:" wsp-name
       (and tag client (not (:selected tag)))
       (do
         (r.awm/toggle-tag wsp-name)
         (awm/focus-client client))

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
