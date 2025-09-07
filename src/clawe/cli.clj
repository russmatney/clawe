(ns clawe.cli
  (:require
   [clojure.string :as string]
   [clawe.config :as clawe.config]))

;; TODO support ops like these with smth like -j
;; take jq insight, read bb-cli for parsing/output inspo
;; use to support scripts like *-in-wsp details
;; first-file, wsp-directory, wsp-git-status
;; related common apps and urls
;; cheatsheets and relevant keybindings

(defn workspace-def [{:as _opts :keys [title]}]
  (clawe.config/workspace-def title))

(defn workspace-dir [opts]
  (-> opts workspace-def :workspace/directory))

(defn preferred-index [opts]
  (-> opts workspace-def :workspace/preferred-index))

(defn git-status [opts]
  (-> opts workspace-def :git/status))

(defn repo-dirs [opts]
  (->>
    (clawe.config/workspace-defs)
    (map (fn [[k def]] [k (:workspace/directory def)]))))

(defn my-repo-dirs [opts]
  (->>
    (clawe.config/workspace-defs)
    (filter (comp :workspace/directory second))
    (filter (fn [[_ def]] (string/includes? (:workspace/directory def) "russmatney")))
    (map (fn [[k def]] [k (:workspace/directory def)]))
    (map str)
    (string/join "\n")
    ;; (into {})
    ))
