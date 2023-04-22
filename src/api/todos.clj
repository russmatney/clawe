(ns api.todos
  (:require
   [clojure.string :as string]
   [garden.db :as garden.db]
   [db.core :as db]
   [garden.core :as garden]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn relevant-org-paths
  "Returns todos in the last 14 days of dailies (if they exist),
  and the last 30 edited garden paths (excluding dailies)."
  []
  (let [daily-paths (garden/daily-paths 14)]
    (->> daily-paths
         (concat
           (->> (garden/last-modified-paths)
                (remove #(do ((into #{} daily-paths) %)))
                (take 30))))))

(defn relevant-org-todos []
  (->>
    (relevant-org-paths)
    garden/paths->flattened-garden-notes
    (filter :org/status)
    (map garden.db/merge-db-item)))

(comment
  (count (relevant-org-paths))
  (count (relevant-org-todos)))

(defn reingest-todos []
  (db/transact (relevant-org-todos)))
