(ns notebooks.git-status
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [ralphie.git :as ralphie.git]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [babashka.fs :as fs]
   [ralphie.zsh :as zsh]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

;; NOTE fetching is not implemented yet, so :needs-pull? will never be set

^::clerk/no-cache
(def clawe-workspace-repos
  (->>
    (clawe.config/workspace-defs-with-titles)
    vals
    (filter :workspace/directory)
    (filter (comp fs/exists? #(str % "/.git") zsh/expand :workspace/directory))
    (map (fn [{:workspace/keys [directory] :as x}]
           (merge x (ralphie.git/status directory))))))

{::clerk/visibility {:result :show}}

;; #### needs push
(clerk/table
  {::clerk/width :full}
  (->> clawe-workspace-repos (filter :git/needs-push?)))

;; #### dirty
(clerk/table
  {::clerk/width :full}
  (->> clawe-workspace-repos (filter :git/dirty?)))

;; #### all dirty

(clerk/table
  {::clerk/width :full}
  (->>
    clawe-workspace-repos
    (filter (fn [{:git/keys [dirty? needs-pull? needs-push?]}]
              (or dirty? needs-pull? needs-push?)))))


;; #### all repos

(clerk/table
  {::clerk/width :full}
  clawe-workspace-repos)
