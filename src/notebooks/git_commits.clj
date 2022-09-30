(ns notebooks.git-commits
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide}}
  (:require
   [nextjournal.clerk :as clerk]
   [git.core :as git]
   [notebooks.clerk :as notebooks.clerk]
   [tick.core :as t]))

^{::clerk/visibility {:result :hide}
  ::clerk/no-cache   true}
(def all-commits
  (->> (git/list-db-commits)
       (sort-by :event/timestamp t/>)))

;; # all commits

(clerk/table
  {::clerk/width :full}
  (->> all-commits
       (map #(select-keys % #{:commit/author-date
                              :commit/directory
                              :commit/short-hash
                              :commit/subject
                              :commit/body}))))

^{::clerk/visibility {:result :hide
                      :code   :hide}
  ::clerk/no-cache   true}
(comment
  (doseq
      ;; TODO maybe opt-in to git status in :workspace/ defs?
      [dirs ["russmatney/clawe"
             "russmatney/org-crud"
             "russmatney/dino"
             "russmatney/dotfiles"]]
    (git/ingest-commits-for-repo
      {:repo/short-path dirs}))

  (notebooks.clerk/update-open-notebooks 'notebooks.git-commits))
