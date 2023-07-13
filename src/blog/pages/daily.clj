(ns blog.pages.daily
  (:require
   [blog.db :as blog.db]
   [blog.item :as item]
   [blog.render :as render]))

(defn note->items [note]
  (some->> note :org/items
           (remove :org/status)
           (filter item/item-has-any-tags)))

(defn note->todos [note]
  (some->> note :org/items
           (filter :org/status)
           (filter item/item-has-any-tags)))

(defn page [note]
  [:div
   [:h1
    {:class ["font-mono"]}
    (:org/name-string note)]

   (item/metadata note)

   (->> note note->items
        (map item/item->hiccup-content)
        (into [:div]))

   [:hr]

   (item/backlink-hiccup note)

   (->> note note->todos
        (map item/item->hiccup-content)
        (into [:div
               [:h3 "Todos"]
               ]))])

(comment
  (blog.db/refresh-notes)
  (let [note
        (->> (blog.db/root-notes)
             (filter :org/name-string)
             (filter (comp #(re-seq #"daily" %) :org/source-file))
             ;; (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
             ;; (filter (comp #(re-seq #"^brainstorm" %) :org/name-string))
             #_(filter (comp #(re-seq #"2022-10-08" %) :org/name-string))
             ;; (filter (comp #(re-seq #"2022-12-04" %) :org/name-string))
             (filter (comp #(re-seq #"2023-07-13" %) :org/name-string))
             first)]
    #_(str (config/blog-content-public) (db/note->uri note))
    #_(page note)
    (render/write-page
      {:note    note
       :content (page note)
       :title   (:org/name note)}))
  )
