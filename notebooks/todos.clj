(ns notebooks.todos
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [org-crud.markdown :as org-crud.markdown]
   [clojure.string :as string]
   [nextjournal.clerk :as clerk]
   [db.core :as db]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

(def all-org-todos
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :org/status _]
        (not [?e :org.prop/archive-file])
        ])
    (map first)
    (sort-by :file/last-modified)
    reverse))


(defn org-item->markdown-str [it]
  (->>
    (org-crud.markdown/item->md-lines it)
    (string/join "\n")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_ "results below here are shown"
{:nextjournal.clerk/visibility {:result :show}}

(clerk/md
  (str "### " (count all-org-todos) " todos"))

(clerk/table
  {::clerk/width :full}
  (->>
    all-org-todos
    (map #(select-keys % [:org/short-path
                          :org/name
                          :org/status
                          :org/tags
                          :org/priority
                          :todo/queued-at]))))

(clerk/md
  {::clerk/width :full}
  (->>
    all-org-todos
    (take 5)
    (map org-item->markdown-str)
    (string/join "\n")))
