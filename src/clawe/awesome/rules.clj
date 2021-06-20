(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require
   [clawe.workspaces :as workspaces]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apply rules to current awesome config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn apply-rules
  "Supports running :rules/apply hooks, which are 0-arg functions that usually
  enforce client/workspace attachments."
  []
  (let [rule-fns
        (->>
          (workspaces/all-workspaces)
          (keep :rules/apply))]
    (notify/notify (str "Applying " (count rule-fns) " Clawe rule(s)"))
    (->> rule-fns
         (map (fn [f] (f)))
         doall)))

(defcom clawe-apply-rules apply-rules)
