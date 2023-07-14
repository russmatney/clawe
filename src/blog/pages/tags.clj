(ns blog.pages.tags
  (:require
   [blog.item :as item]
   [blog.db :as blog.db]
   [blog.render :as render]))

(defn notes-by-tag [notes]
  (->> notes
       (map #(dissoc % :org/body))
       (reduce
         (fn [by-tag note]
           (reduce
             (fn [by-tag tag]
               (update by-tag tag (fnil
                                    (fn [notes]
                                      (conj notes note))
                                    #{note})))
             by-tag
             (let [tags (item/item->all-tags note)]
               (if (seq tags) tags
                   ;; here we include items with no tags
                   ;; these should be given tags, and
                   ;; helps to surface them
                   [nil]))))
         {})
       (sort-by (comp (fn [x] (if x (count x) 0)) second) >)))

(defn tag-block [{:keys [tag notes]}]
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h3
     {:class ["pb-2" "pointer-cursor"]}
     [:a {:id tag}
      (or tag "Untagged")]]]

   (when (seq notes)
     (->> notes (map #(item/note-row % {:tags #{tag}})) (into [:div])))
   [:hr]])

(defn all-tags []
  (->> (blog.db/all-notes) (mapcat :org/tags) (into #{})))

(comment
  (->> (all-tags)
       (sort)
       (blog.item/tags-list-terms)))

(defn tag-pool []
  (let [tags (all-tags)]
    (blog.item/tags-list (sort tags))))

(defn page []
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h2 {:class ["font-mono"]} "Notes By Tag"]]

   (->>
     (notes-by-tag (blog.db/published-notes))
     (map (fn [[tag notes]]
            (when (and tag (seq notes))
              (tag-block {:tag tag :notes notes}))))
     (into [:div (tag-pool)]))])

(comment
  (render/write-page
    {:path    "/tags.html"
     :content (page)
     :title   "Home"}))
