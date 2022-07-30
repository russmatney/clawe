(ns expo.core
  (:require
   [expo.db :as expo.db]
   [garden.db :as garden.db]
   [defthing.db :as db]))

(def ignored-tags #{"archived"})

(defn notes-for-tags [tags]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?tags ?ignored-tags
                :where
                [?e :doctor/type :type/garden]
                ;; TODO parse file-tags to :org/tags in org-crud
                ;; [?e :org/level :level/root]
                [?e :org/tags ?tag]
                [(?tags ?tag)]
                [(not (contains? ?ignored-tags ?tag))]]
              tags ignored-tags)
    (map first)))

(def post-tags #{"til" "post" "posts" "blog"})

(defn note->expo-post [{:keys [] :as note}]
  (-> note
      (assoc :expo/type :type/post)
      (assoc :expo/title (:org/name note))))

(defn update-posts []
  (->>
    (notes-for-tags post-tags)
    (map note->expo-post)
    (expo.db/transact)))

(comment

  (update-posts)



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
    (expo.db/transact)))
