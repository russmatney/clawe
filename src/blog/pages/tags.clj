(ns blog.pages.tags
  (:require
   [blog.item :as item]
   [blog.db :as blog.db]
   [blog.render :as render]
   [clojure.string :as string]))

;; data

(defn notes-by-tag []
  ;; TODO use local database
  (->> (blog.db/published-notes)
       (map #(dissoc % :org/body))
       (reduce
         (fn [by-tag note]
           (reduce #(update %1 %2 (fnil (fn [notes] (conj notes note)) #{note}))
                   by-tag
                   (let [tags (item/item->published-tags note)]
                     (when (seq tags) tags))))
         {})))

(defn tag-anchor-groups-by-letter [tag->notes]
  (->> tag->notes
       keys
       (remove nil?)
       (group-by (comp string/lower-case first))
       (sort-by first)
       (map (fn [[letter tags]]
              [letter
               (->> tags
                    sort
                    (map (fn [tag]
                           ;; build for anchor-href-list
                           {:tag tag
                            :n   (count (tag->notes tag))})))]))))

(defn data []
  (let [by-tag (notes-by-tag)]
    {:highest-count-first (->> by-tag
                               (sort-by (comp (fn [x] (if x (count x) 0)) second) >))
     :alphabetical        (->> by-tag
                               (remove (comp nil? first))
                               (sort-by (comp string/lower-case first)))
     :tag-groups          (tag-anchor-groups-by-letter by-tag)}))


(comment
  (notes-by-tag)
  (data)
  (:tag-groups (data)))

;; components

(defn tag-block [{:keys [tag notes]}]
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h3
     {:class ["pb-2"]}
     [:a {:id tag}
      (or tag "Untagged")]]

    [:a     {:class ["cursor-pointer"
                     "ml-auto"]
             ;; back to top
             :href  "/tags.html#"}
     "^top"]]

   (when (seq notes)
     (->> notes (map #(item/note-row % {:tags #{tag}})) (into [:div])))
   [:hr]])

(defn tag-anchor-groups [tag-groups]
  (for [[tag tags] tag-groups]
    [:div
     {:class ["pt-4"]}
     [:h3 {:class ["flex" "flex-row" "justify-center"]} tag]
     (blog.item/tags-href-list tags)]))

(defn page []
  (let [{:keys [alphabetical tag-groups]} (data)]
    [:div
     [:div
      {:class ["flex" "flex-row" "justify-center"]}
      [:h2 {:class ["font-mono"]} "Notes By Tag"]]

     (->> alphabetical
          (map (fn [[tag notes]]
                 (when (and tag (seq notes))
                   (tag-block {:tag tag :notes notes}))))
          (into [:div
                 (tag-anchor-groups tag-groups)
                 [:hr]]))]))

(comment
  (render/write-page
    {:path    "/tags.html"
     :content (page)
     :title   "Home"}))
