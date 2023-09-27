(ns blog.pages.daily
  (:require
   [blog.db :as blog.db]
   [blog.item :as item]
   [blog.render :as render]

   [org-crud.core :as org-crud]
   ))

(defn note->items [note]
  (some->> note :org/items
           (remove :org/status)
           (filter item/item-has-any-tags)))

(defn note->all-todos [note]
  (some->> note :org/items
           (filter item/item-has-any-tags)
           (mapcat item/item->all-todos)))

(defn toc [note]
  (->> note
       note->items
       (mapcat org-crud/nested-item->flattened-items)
       (remove :org/status)
       (map (fn [item]
              [:h3
               {:class ["not-prose"]}
               [:a {:href (str "#" (item/item->anchor-link item))}
                (:org/name-string item)]]))))

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

   (->> note note->items
        (map #(item/item->hiccup-content % {:filter-fn (comp not :org/status)}))
        (into [:div]))

   [:hr]

   (item/backlink-hiccup note)

   (when-let [todos (seq (note->all-todos note))]
     (->> todos
          ;; TODO consider a new todo renderer that exposes parent-names

          (map #(item/item->hiccup-content
                  ;; skip children b/c we all ready fetch them
                  % {:skip-children true}))
          (into [:div
                 [:hr]
                 [:h3 "Todos"]])))])

(comment
  (blog.db/refresh-notes)
  (let [note
        (->> (blog.db/root-notes)
             (filter :org/name-string)
             (filter (comp #(re-seq #"daily" %) :org/source-file))
             ;; (filter (comp #(re-seq #"Things I Love" %) :org/name-string))
             ;; (filter (comp #(re-seq #"^brainstorm" %) :org/name-string))
             #_(filter (comp #(re-seq #"2022-11-21" %) :org/name-string))
             (filter (comp #(re-seq #"2023-09-25" %) :org/name-string))
             first)]
    (def note note)
    #_(str (config/blog-content-public) (db/note->uri note))
    (page note)
    (render/write-page
      {:note    note
       :content (page note)
       :title   (:org/name note)})))
