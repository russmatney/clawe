(ns blog.pages.daily
  (:require
   [org-crud.core :as org-crud]
   [garden.core :as garden]

   [blog.config :as blog.config]
   [blog.db :as blog.db]
   [blog.item :as item]
   [blog.render :as render]))

(defn note->daily-items [note]
  (some->> note :org/items (filter item/item-has-any-tags)))

(defn page [note]
  [:div
   [:h1
    {:class ["font-mono"]}
    (:org/name-string note)]

   (item/metadata note)

   (->> note note->daily-items
        (map item/item->hiccup-content)
        (into [:div]))

   (item/backlink-hiccup note)])

(comment
  (blog.db/refresh-notes)
  (let [note
        #_ (-> (garden/daily-path #_2) org-crud/path->nested-item)
        (->> (blog.db/root-notes)
             (filter :org/name-string)
             (filter (comp #(re-seq #"daily" %) :org/source-file))
             ;; (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
             ;; (filter (comp #(re-seq #"^brainstorm" %) :org/name-string))
             (filter (comp #(re-seq #"2022-10-08" %) :org/name-string))
             (filter (comp #(re-seq #"2022-12-04" %) :org/name-string))
             first)]
    #_(str (config/blog-content-public) (db/note->uri note))
    #_(page note)
    (render/write-page
      {:path    (str (blog.config/blog-content-public) (blog.db/note->uri note))
       :content (page note)
       :title   (:org/name note)})))

