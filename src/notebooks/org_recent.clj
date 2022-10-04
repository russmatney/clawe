(ns notebooks.org-recent
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [garden.core :as garden]
   [org-crud.core :as org-crud]
   [org-crud.markdown :as org-crud.markdown]
   [clojure.string :as string]
   [nextjournal.clerk :as clerk]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

(def recent-org-items
  (->>
    (garden/last-modified-org-paths)
    (take 10)
    (map (comp org-crud/path->nested-item
               :org/source-file))))

(defn org-item->markdown-str [it]
  (->>
    (org-crud.markdown/item->md-body it)
    (string/join "\n")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{::clerk/visibility {:result :show}}

(clerk/md
  (->>
    recent-org-items
    (map org-item->markdown-str)
    (string/join "\n---\n")))
