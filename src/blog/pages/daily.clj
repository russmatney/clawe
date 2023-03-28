(ns blog.pages.daily
  (:require
   [org-crud.core :as org-crud]
   [garden.core :as garden]

   [blog.config :as blog.config]
   [blog.db :as blog.db]
   [blog.item :as item]
   [blog.render :as render]))

(def ^:dynamic *note*
  (-> (garden/daily-path #_2) org-crud/path->nested-item))

(defn note->daily-items [note]
  (some->> note :org/items (filter item/item-has-any-tags)))

(defn page [note]
  [:div
   [:h1 [:code (:org/name note)]]

   (->> note note->daily-items
        (map item/item->hiccup-content)
        (into [:div]))

   (item/id->backlink-hiccup (:org/id *note*))])

(comment
  (render/write-page
    {:path    (str (blog.config/blog-content-public) (blog.db/note->uri *note*))
     :content (page *note*)
     :title   (:org/name *note*)}))
