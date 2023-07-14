(ns blog.pages.note
  (:require
   [blog.item :as item]
   [blog.db :as db]
   [blog.render :as render]))

(defn note->todos [note]
  (some->> note blog.item/item->all-todos))

(defn page [note]
  [:div
   [:h1
    {:class ["font-mono"]}
    (:org/name-string note)]
   (item/metadata note)

   (item/item->hiccup-content
     note {:skip-title true
           :filter-fn  (comp not :org/status)})

   (when-let [backlinks (seq (item/backlink-hiccup note))]
     [:span
      [:hr]
      backlinks])

   (when-let [todos (seq (note->todos note))]
     (->> todos
          (map item/item->hiccup-content)
          (into [:div
                 [:hr]
                 [:h3 "Todos"]])))])


(comment
  (db/refresh-notes)
  (let [note (->> (db/root-notes)
                  (filter :org/name-string)
                  ;; (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^brainstorm" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^future" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^clojure$" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^clawe$" %) :org/name-string))
                  (filter (comp #(re-seq #"^Juice in Games" %) :org/name-string))
                  first
                  )]
    #_(str (config/blog-content-public) (db/note->uri note))
    #_(page note)
    (render/write-page
      {:note    note
       :content (page note)
       :title   (:org/name note)})))
