(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require
   [clawe.workspaces :as workspaces]
   [clojure.string :as string]
   [clojure.walk :as walk]))

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

    (spit "/home/russ/.config/awesome/workspace-rules.fnl" file-contents)))

(comment
  (write-awesome-rules))
