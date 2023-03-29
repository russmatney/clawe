(ns blog.pages.note
  (:require
   [blog.item :as item]
   [blog.db :as db]
   [blog.render :as render]
   [blog.config :as config]))

(defn page [note]
  [:div
   [:h1
    {:class ["font-mono"]}
    (:org/name-string note)]
   (item/metadata note)
   (item/item->hiccup-content note {:skip-title true})
   (item/backlink-hiccup note)])


(comment
  (db/refresh-notes)
  (let [note (->> (db/root-notes)
                  (filter :org/name-string)
                  ;; (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^brainstorm" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^future" %) :org/name-string))
                  (filter (comp #(re-seq #"^clojure$" %) :org/name-string))
                  first)]
    #_(str (config/blog-content-public) (db/note->uri note))
    #_(page note)
    (render/write-page
      {:path    (str (config/blog-content-public) (db/note->uri note))
       :content (page note)
       :title   (:org/name note)})))
