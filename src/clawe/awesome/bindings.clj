(ns clawe.awesome.bindings
  "Manages awesomeWM bindings derived from clawe in-memory data structures."
  (:require
   [clawe.bindings :as bindings]
   [clojure.string :as string]
   [clawe.awesome :as awm]))

(def modifiers {:mod     "Mod4"
                :shift   "Shift"
                :control "Control"})

(defn binding->awful-key
  [{:keys [binding/key binding/command]}]
  (let [[mods key] key
        mods       (->> mods
                        (map modifiers)
                        (map #(str "\"" % "\"")))]
    (str
      "(awful.key "
      "[ " (string/join " " mods) " ] \"" key "\" "
      "(fn [] "
      "(local naughty (require :naughty)) "
      "(naughty.notify {:title \"My Binding\" :text \"key pressed\"}) "
      "))")))

(defn append-global-keybindings []
  ;; TODO this adds a listener for each key, but does not clear any existing...
  (let [awful-keys (->> (bindings/list-bindings)
                        (map binding->awful-key)
                        (string/join " "))]
    (str "(awful.keyboard.append_global_keybindings [" awful-keys "] )")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update the bindings in-place
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-awesome-bindings []
  (let [global (append-global-keybindings)]
    (awm/awm-fnl global)))

(comment
  (update-awesome-bindings)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write out code that awesome loads at startup/restart
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn write-awesome-bindings []
  (let [global        (append-global-keybindings)
        file-contents (str ";; global bindings\n" global)]
    (spit "/home/russ/.config/awesome/clawe-bindings.fnl" file-contents)))

(comment
  (->> (bindings/list-bindings)
       (map binding->awful-key))

  (write-awesome-bindings))
