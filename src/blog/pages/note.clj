(ns blog.pages.note
  (:require
   [blog.item :as item]
   [blog.db :as db]
   [blog.render :as render]
   [blog.config :as config]))

(defn page [note]
  [:div
   ;; TODO show metadata
   ;; TODO show last modified
   ;; TODO show date published
   (item/item->hiccup-content note)
   (item/id->backlink-hiccup (:org/id note))])


(comment
  (let [note (->> (db/root-notes)
                  (filter :org/name-string)
                  (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
                  first)]
    (render/write-page
      {:path    (str (config/blog-content-public) (db/note->uri note))
       :content (page note)
       :title   (:org/name note)})))
