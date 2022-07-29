(ns expo.core
  (:require
   [datascript.core :as d]
   [ralphie.zsh :as zsh]
   [garden.db :as garden.db]
   [defthing.db :as db]))

;; TODO config
(def expo-db-path (zsh/expand "~/russmatney/clawe/expo/public/expo-db.edn"))

;; TODO systemic
(def conn
  (->
    (d/empty-db)
    (d/conn-from-db)))

(defn print-db []
  (pr-str (d/db conn)))

(defn write-db-to-file []
  (spit expo-db-path (print-db)))


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

  (d/transact! conn [{:some/new   :piece/of-data
                      :with/attrs 23
                      :and/other  :attrs/enum}])

  ;; pr-str the db to get it stringified
  (pr-str (d/db conn))

  (def db-str (pr-str (d/db conn)))
  (spit expo-db-path db-str)

  (garden.db/fetch-db-garden-notes)


  (def org-tags
    (->>
      (db/query '[:find ?tag
                  :where [_ :org/tags ?tag]])
      (map first)
      (into #{})))

  (->>
    org-tags
    sort)

  (def ignore-tags #{"archived"})

  (notes-for-tags #{"til" "post" "posts" "blog"})




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


  (d/transact! conn notes-tagged-post)

  (pr-str (d/db conn))


  (->>
    (notes-for-tags #{"til"})
    (d/transact! conn)
    )

  (write-db-to-file)



  )
