(ns blog.pages.note
  (:require
   [blog.item :as item]
   [blog.db :as db]
   [blog.render :as render]
   [org-crud.core :as org-crud]
   ))

(defn note->all-todos [note]
  (some->> note blog.item/item->all-todos))

(defn toc [note]
  (let [uri (blog.db/id->link-uri (:org/id note))]
    (->> note
         :org/items
         (remove :org/status)
         (mapcat org-crud/nested-item->flattened-items)
         (remove :org/status)
         (filter (comp #{1} :org/level-int))
         (map #(item/note-child-row {:uri uri} %)))))

(defn page [note]
  [:div
   [:h1
    {:class ["font-mono"]}
    (:org/name-string note)]

   (item/metadata note)

   (into
     [:div
      {:class []}
      [:h3 "Contents"]]
     (toc note))

   [:hr]

   (item/item->hiccup-content
     note {:skip-title true
           :filter-fn  (comp not :org/status)})

   [:hr]

   (item/backlink-hiccup note)

   (when-let [todos (seq (note->all-todos note))]
     (->> todos
          (map #(item/item->hiccup-content % {:skip-children true}))
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
                  ;; (filter (comp #(re-seq #"^Juice in Games" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^Beat Em Up City \(dino" %) :org/name-string))
                  ;; (filter (comp #(re-seq #"^Fennel index" %) :org/name-string))
                  (filter (comp #(re-seq #"^prototype jam" %) :org/name-string))
                  first
                  )]
    #_(str (config/blog-content-public) (db/note->uri note))
    #_(page note)
    #_(db/note->uri note)
    (render/write-page
      {:note    note
       :content (page note)
       :title   (:org/name note)})))



