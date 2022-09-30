(ns notebooks.git-status
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide}}
  (:require
   [ralphie.git :as ralphie.git]
   [nextjournal.clerk :as clerk]
   [clawe.config :as clawe.config]
   [babashka.fs :as fs]
   [ralphie.zsh :as zsh]))

;; NOTE fetching is not implemented yet, so :needs-pull? will never be set

^{::clerk/visibility {:result :hide}
  ::clerk/no-cache   true}
(def clawe-workspace-repos
  (->>
    (clawe.config/workspace-defs-with-titles)
    vals
    (filter :workspace/directory)
    (filter (comp fs/exists? #(str % "/.git") zsh/expand :workspace/directory))
    (map (fn [{:workspace/keys [directory] :as x}]
           (merge x (ralphie.git/status directory))))))

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

;; ## [/clerk.clj](/notebooks/clerk)
;; ## [/clawe.clj](/notebooks/clawe)
;; ## [/git-commits.clj](/notebooks/git-commits)
;; ## [/git-status.clj](/notebooks/git-status)
