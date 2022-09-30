^{:nextjournal.clerk/visibility {:code :hide}}
(ns doctor.garden
  (:require
   [nextjournal.clerk :as clerk]
   [notebooks.clerk :as notebooks.clerk]
   [db.core :as db]))

;; # Garden

;; # Recent org

^{::clerk/visibility {:code :hide :result :hide}
  ::clerk/no-cache   true
  }
(def recent-org-files
  (->>
    (db/query '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/garden]
                [?e :org/source-file ?src]])
    (map first)
    (filter :org.prop/created-at)
    (sort-by :org.prop/created-at)
    reverse
    (take 10)))

^{::clerk/visibility {:code :hide}}
(clerk/table recent-org-files)


(clerk/example
  recent-org-files)

(comment
  (notebooks.clerk/update-open-notebooks))
