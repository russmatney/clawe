(ns clawe.awesome.rules
  "Manages the awesomeWM rules."
  (:require
   [clawe.wm :as wm]
   [clojure.string :as string]
   [clojure.walk :as walk]
   [ralphie.zsh :as zsh]))

(defn awm-workspace-rules
  "Returns values intended as literal awesome/rules.
  The first arg is always expected to be the relevant workspace.

  Supports two versions:
  - Single arity returns a simple rule.
  - Multi-arity implies a broader match-any."
  ([name]
   {:rule       {:name name}
    :properties {:tag name}})
  ([name & aliases]
   (let [all (cons name aliases)]
     {:rule_any   {:class all :name all}
      :properties {:tag name :first_tag name}})))

(comment
  (awm-workspace-rules "emacs")
  (awm-workspace-rules "emacs" "Emacs"))

(defn workspace->awm-rules [{:workspace/keys [title app-names]}]
  (apply awm-workspace-rules title app-names))

(comment
  (workspace->awm-rules {:workspace/title "spotify"})
  (workspace->awm-rules {:workspace/title     "spotify"
                         :workspace/app-names ["Spotify" "Pavucontrol"]}))

(def awesome-rules-path (zsh/expand "~/.config/awesome/workspace-rules.fnl"))

(defn write-awesome-rules
  "Converts all workspace defs into awesome-rules, dumped into `workspace-rules.fnl`.

  The awesome config then reads this file into its rules."
  []
  (let [wsp-rules (->> (wm/workspace-defs)
                       (map workspace->awm-rules)
                       (walk/postwalk (fn [x]
                                        (if (seq? x)
                                          (into [] x)
                                          x)))
                       (string/join "\n")
                       (#(string/replace % "," "")))
        file-contents
        (str "{:all [" wsp-rules "]}")]

    (spit awesome-rules-path file-contents)))
