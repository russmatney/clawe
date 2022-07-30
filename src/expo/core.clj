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

(defn notes-matching-source-file-reg [reg]
  (->>
    (db/query '[:find (pull ?e [*])
                :in $ ?ignored-tags ?reg
                :where
                [?e :doctor/type :type/garden]
                ;; TODO parse file-tags to :org/tags in org-crud
                [?e :org/level :level/root]
                ;; [?e :org/tags ?tag]
                ;; [(not (contains? ?ignored-tags ?tag))]

                [?e :org/source-file ?src-file]
                [(re-seq ?reg ?src-file)]]
              ignored-tags reg)
    (map first)
    (sort-by :org/source-file)
    (reverse)
    ))

(comment
  (->>
    (notes-matching-source-file-reg #"/daily/2022-0[6|7]")
    (take 10)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; collecting for tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def post-tags #{"til" "post" "posts" "blog"})

(defn note->expo-post [{:keys [] :as note}]
  (-> note
      (assoc :expo/type :type/post)
      (assoc :expo/title (:org/name note))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; collecting for dailies
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
