(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require [clawe.awesome :as awm]
            [clawe.workspaces :as workspaces]
            [clojure.string :as string]
            [clojure.walk :as walk]))

(comment
  (println "howdy")

  ;; trying to read the rules here, not yet parseable
  (awm/awm-fnl
    '(view awful.rules.rules))
  )

(defn write-awesome-rules []
  (let [wsp-rules
        (->> (workspaces/all-workspaces)
             (keep :awesome/rules)
             (walk/postwalk (fn [x]
                              (if (seq? x)
                                (into [] x)
                                x)))
             (string/join "\n")
             (#(string/replace % "," "")))
        file-contents
        (str "{:all [" wsp-rules "]}")]

    (spit "/home/russ/.config/awesome/workspace-rules-magic.fnl" file-contents)))

(comment
  (write-awesome-rules))
