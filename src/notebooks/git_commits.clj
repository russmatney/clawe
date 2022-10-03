(ns notebooks.git-commits
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [nextjournal.clerk :as clerk]
   [git.core :as git]
   [notebooks.clerk :as notebooks.clerk]
   [tick.core :as t]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [notebooks.nav :as nav]))

^{::clerk/no-cache true}
(def all-commits
  (->> (git/list-db-commits)))

^{::clerk/no-cache true}
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{:nextjournal.clerk/visibility {:result :show}}

;; # Git Commits

(clerk/table
  {::clerk/width :full}
  (->> all-commits
       (sort-by :event/timestamp t/>)
       (map #(select-keys
               % #{:commit/author-date
                   :commit/directory
                   :commit/short-hash
                   :commit/subject
                   :commit/body}))
       (map (fn [commit]
              (update commit
                      :commit/directory
                      #(string/replace % (str (fs/home)) "~"))))))

;; NOTE these do not end up in the TOC :/
(clerk/md
  (nav/notebook-links))
