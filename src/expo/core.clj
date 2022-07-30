(ns expo.core
  (:require
   [expo.db :as expo.db]
   [garden.db :as garden.db]
   [defthing.db :as db]))


(defn notes-for-tags [tags]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?tags
                :where
                [?e :doctor/type :type/garden]
                ;; TODO parse file-tags to :org/tags in org-crud
                ;; [?e :org/level :level/root]
                [?e :org/tags ?tag]
                [(?tags ?tag)]]
              tags)
    (map first)))


(comment
  (notes-for-tags #{"til" "post" "posts" "blog"})

  (garden.db/fetch-db-garden-notes)


  (def org-tags
    (->>
      (db/query '[:find ?tag
                  :where [_ :org/tags ?tag]])
      (map first)
      (into #{})))
  (->> org-tags sort)

  (def notes-tagged-til
    (->>
      (db/query '[:find (pull ?e [*])
                  :where [?e :org/tags "til"]])
      (map first)))

  (def notes-tagged-post
    (->>
      (db/query '[:find (pull ?e [*])
                  :where
                  [?e :org/tags ?tag]
                  [(contains? #{"post" "posts"} ?tag)]
                  [?e :org/level :level/root]
                  ])
      (map first)))


  (expo.db/transact notes-tagged-post)

  (->>
    (notes-for-tags #{"til"})
    (expo.db/transact))

  )
