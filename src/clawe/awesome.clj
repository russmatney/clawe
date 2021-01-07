(ns clawe.awesome
  (:require
   [babashka.process :refer [$ check]]))

(def lua-preamble "awful = require('awful');")

(defn awm-cli
  "Expects `lua-str`, a literal string of lua.

  Adds a preamble that sets common variables and requires common modules.
  "
  [lua-str]
  (->>
    (str lua-preamble ";\n" lua-str)
    ((fn [lua-str]
       (println "Running lua via awesome-client!:\n\n" lua-str)
       lua-str))
    ((fn [lua-str]
       (->
         ^{:out :string}
         ($ awesome-client ~lua-str)
         check
         :out)))))

(comment
  (awm-cli "return 'hi';"))
