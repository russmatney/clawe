(ns clawe.sxhkd.bindings
  "Manages bindings for sxhkd derived from clawe.bindings."
  (:require
   [clawe.bindings :as bindings]
   [clojure.string :as string]
   [babashka.process :as proc]
   [ralphie.notify :as notify]))

(defn log [msg]
  ;; TODO consider clawe.log macro
  (let [msg (str "[CLAWE] " msg)]
    (println msg)
    (notify/notify
      {:notify/id      :clawe.sxhkd
       :notify/subject msg
       :notify/body    :clawe.sxhkd})))


(def modifiers {:mod     "super"
                :alt     "alt"
                :shift   "shift"
                :ctrl    "ctrl"
                :control "ctrl"})

(def keyname {"." "period"
              "," "comma"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update bindings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn binding->sxhkd-key
  "Maps a clawe keybinding to a call to `awful.key`."
  [{:keys [binding/key binding/raw-fnl binding/awm] :as bd}]
  (let [[mods key] key
        mods       (->> mods (map modifiers))
        key        (keyname key key)]
    ;; TODO rewrite so that the bindings impls aren't aware of each other
    (when-not (or raw-fnl awm)
      (str
        (string/join " + " mods) (when (seq mods) " + ") key
        "\n  " (bindings/binding-cli-command bd)))))

(comment
  (binding->sxhkd-key
    {:binding/key          [[:mod] "Yah!"]
     ;; :binding/raw-fnl      '(let [] (pp "yah!"))
     :binding/command-name "yah-command"}))

(defn raw-sxhkdrc
  "Returns an sxhkdrc config file."
  []
  (->> (bindings/list-bindings)
       (map binding->sxhkd-key)
       (remove nil?)
       (string/join "\n\n")))

(defn reset-bindings []
  (log "SXHKD bindings resetting...")
  (let [config (raw-sxhkdrc)]
    (spit "/home/russ/.config/sxhkd/sxhkdrc" config)
    (log "SXHKD bindings rewritten.")
    (-> (proc/$ systemctl --user restart sxhkd)
        (proc/check))
    (log "SXHKD bindings reset.")))

(comment
  (reset-bindings)
  )
