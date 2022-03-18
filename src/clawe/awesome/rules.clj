(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require
   [clawe.defs.workspaces :as defs.workspaces]
   [clawe.workspaces :as workspaces]
   [clojure.string :as string]
   [clojure.walk :as walk]))

(defn valid-rules? [wsp]
  (when-let [rules (:awesome/rules wsp)]
    (and
      (-> rules :properties :tag nil? not)
      (or
        (-> rules :rule :name nil? not)
        (-> rules :rule_any :class nil? not)
        (-> rules :rule_any :name nil? not)
        (-> rules :rule_any :name nil? not)))))

(defn ensure-awm-rules [wsp]
  (if (valid-rules? wsp)
    ;; if already set some valid rules, use those
    wsp
    (merge wsp (defs.workspaces/awesome-rules wsp))) )

(comment
  (->> (workspaces/all-workspaces)
       (map ensure-awm-rules)
       ;; count
       ;; (remove valid-rules?)
       count
       ;; first
       )

  (->> (workspaces/all-workspaces)
       (filter (comp nil? :name :rule :awesome/rules))))


(defn write-awesome-rules
  "Converts all-workspaces into awesome-rules, dumped into `workspace-rules.fnl`.

  The awesome config should read this file into it's rules.

  Dynamically ensures a basic awesome-rule config for the workspace if a missing
  or invalid one is set (on :awesome/rules) via `valid-rules?` and
  `ensure-awm-rules`."
  []
  (let [wsp-rules
        (->> (workspaces/all-workspaces)
             (map ensure-awm-rules)
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
  (write-awesome-rules)

  (->> (workspaces/all-workspaces)
       (filter (comp #{"aave"} :name))
       first))
