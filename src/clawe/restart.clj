(ns clawe.restart
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [clawe.install :as c.install]
   [babashka.process :as proc]

   [clawe.awesome :as awm]
   [clawe.awesome.rules :as awm.rules]
   [clawe.awesome.bindings :as awm.bindings]
   [clawe.bindings :refer [defkbd]]
   [clawe.sxhkd.bindings :as sxhkd.bindings]))

(defn log [msg]
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.restart
       :notify/subject msg
       :notify/body    :clawe.restart})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom restart
  "Rebuild the uberjar exec `clawe rewrite-and-reload`.

We want to make sure the new config is written with the updated
uberjar."
  (do
    (log "restarting...")
    ;; maybe detect if the current uberjar is out of date?
    ;; might not always need to rebuild here
    (c.install/build-uberjar)
    (log "rebuilt uberjar...")
    ;; maybe a pause or file read/watch, something that shows it's new?

    ;; if nothing has changed, maybe just call rewrite-and-reload from here
    (log "reloading...")
    (->
      ;; kicking to process here could mean we use the new uberjar
      ;; (if the prev command waits for completion, which it seems to)
      ;; Also helps avoid a circular dep, b/c reload depends on these bnds
      ^{:out :inherit}
      (proc/$ clawe reload)
      proc/check :out slurp)))

(defkbd clawe-restart
  [[:mod] "r"]
  {:binding/awm true}
  ;; TODO support just passing in a defcom
  ;; TODO consider moving keybindings into domain files?
  (defcom/exec restart))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom reload
  "Write all dependent configs and restart whatever daemons."
  (do
    (log "reloading...")

    ;; Bindings
    (log "rewriting awm bindings")
    (awm.bindings/write-awesome-bindings)
    (log "resetting sxhkd bindings")
    (sxhkd.bindings/reset-bindings)

    ;; Rules
    (log "rewriting rules")
    (awm.rules/write-awesome-rules)
    (log "reapplying rules")
    (awm.rules/apply-rules)

    ;; Misc
    (log "reloading misc")
    (awm/reload-misc)

    ;; Widgets
    (log "reloading widgets")
    (awm/reload-bar-and-widgets)

    (log "completed.")))

(comment
  (defcom/exec reload))
