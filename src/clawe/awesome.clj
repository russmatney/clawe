(ns clawe.awesome
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]))

(defn parse-output
  "Parses the output of awm-cli, with assumptions not worth baking into the root
  command."
  [str]
  (->>
    ;; remove leading `string` label
    (-> str string/trim (string/replace #"^string " ""))

    ;; drop quotes
    (drop 1) reverse
    (drop 1) reverse

    ;; rebuild string
    string/join

    ((fn [s]
       (try
         ;; convert to clojure data structure
         ;; TODO: use edn/read-string?
         (load-string s)
         (catch Exception _e
           (println "Exception while parsing output:" s)
           s))))))

(def lua-preamble
  "Passed ahead of awm-cli commands to provide common vars."
  "-- Preamble
awful = require('awful');
lume = require('lume');
view = require('fennelview');
inspect = require 'inspect';
s = awful.screen.focused();
lain = require 'lain';")

(defn awm-cli
  "Expects `lua-str`, a literal string of lua.
  Adds a preamble that sets common variables and requires common modules."
  [lua-str]
  (->>
    (str lua-preamble "\n\n-- Passed command:\n" lua-str)
    ((fn [lua-str]
       (println "Running lua via awesome-client!:\n\n" lua-str)
       lua-str))
    ((fn [lua-str]
       ^{:out :string}
       ($ awesome-client ~lua-str)))
    check
    :out
    parse-output))

(comment
  (awm-cli "return 'hi';")

  (awm-cli
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name} end))"))

  (println "hello")
  (awm-cli "print('hello')")
  (awm-cli "return view(lume.map(s.tags, function (t) return {name= t.name} end))"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload Widgets
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def widget-filenames
  ;; TODO generate this from the awesome/widgets/* dir
  #{"workspaces"
    "org-pomodoro"
    "focus"
    "pomodoro"
    "dirty-repos"})

(defn hotswap-widget-modules []
  (->> widget-filenames
       (map (partial str "widgets."))
       (map #(str "lume.hotswap('" % "');"))
       (cons "lume.hotswap('bar');")
       reverse ;; move 'bar' hotswap to last
       (string/join "\n")
       (awm-cli)))

(defn init-screen []
  (awm-cli "require('bar'); init_screen();"))

(defn reload-widgets []
  (hotswap-widget-modules)
  (init-screen))
