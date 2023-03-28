(ns blog.pages.note
  (:require
   [blog.item :as item]
   [blog.db :as db]
   [blog.render :as render]
   [blog.config :as config]))

(def ^:dynamic *note*
  (->> (db/root-notes)
       ;; (remove (comp #(string/includes? % "/daily/") :org/source-file))
       (sort-by :file/last-modified)
       last))

(defn page [note]
  [:div
   ;; TODO show metadata
   ;; TODO last modified
   (item/item->hiccup-content note)
   (item/id->backlink-hiccup (:org/id *note*))])


(comment
  (render/write-page
    {:path    (str (config/blog-content-public) (db/note->uri *note*))
     :content (page *note*)
     :title   (:org/name *note*)}))
