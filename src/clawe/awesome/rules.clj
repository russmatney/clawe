(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require
   [clawe.awesome :as awm]
   [clawe.workspaces :as workspaces]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [ralph.defcom :refer [defcom]]))

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

    (spit "/home/russ/.config/awesome/workspace-rules.fnl" file-contents)))

(comment
  (write-awesome-rules))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apply rules to current awesome config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn apply-rules
  "Supports running :rules/apply hooks, which are 0-arg functions."
  []
  (->>
    (workspaces/all-workspaces)
    (keep :rules/apply)
    (map (fn [f] (f)))
    doall))

(defcom apply-rules-cmd
  {:defcom/name "clawe-apply-rules"
   :defcom/handler (fn [_ _] (apply-rules))})
