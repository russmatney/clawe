(ns blog.pages.index
  (:require
   [tick.core :as t]
   [clojure.set :as set]

   [dates.tick :as dates]
   [blog.item :as blog.item]
   [blog.render :as render]
   [blog.db :as blog.db]))

(defn notes-by-day [notes]
  (->> notes
       (filter :file/last-modified)
       (map #(dissoc % :org/body))
       (group-by #(-> % :file/last-modified dates/parse-time-string t/date))
       (map (fn [[k v]] [k (into [] v)]))
       (sort-by first t/>)))

(defn day-block [{:keys [day notes]}]
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h3
     {:class ["pb-2"]}
     (t/format (t/formatter "EEEE, MMM dd") day)]]

   (when (seq notes)
     (->> notes (map blog.item/note-row) (into [:div])))
   [:hr]])

(defn page []
  [:div

   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h2 {:class ["font-nes"]} "Home"]]

   [:div
    [:h3 [:a {:href "/note/blog_about.html"} "About"]]]

   [:div
    [:h3 "Notes"]
    [:div
     {:class "pl-4"}
     [:h3
      [:a {:href "/last-modified.html"} "...by Last Modified"]]
     [:h3
      [:a {:href "/tags.html"} "...by Tag"
       ;; TODO include 5 most common tags
       ]]]]

   [:div
    [:h3 "Projects"]
    (->> (blog.db/published-notes)
         (filter (comp seq
                       #(set/intersection
                          #{"project" "projects"} %)
                       :org/tags))
         ;; TODO sort projects, include tags
         (map (fn [note]
                ;; TODO include link to repo
                ;; TODO include short description
                (blog.item/note-row note)))
         (into [:div {:class "pl-4"}]))]

   #_ [:div
       [:h3 "Commits"]
       (->> (blog.db/published-notes)
            (filter (comp seq
                          #(set/intersection
                             #{"project" "projects"} %)
                          :org/tags))
            (filter :org.prop/repo)
            (map (fn [note]
                   (let [repo (:org.prop/repo note)]
                     [:h3 repo])))
            (into [:div {:class "pl-4"}]))]

   [:div
    [:h3 "Posts"]
    (->> (blog.db/published-notes)
         (filter (comp seq
                       #(set/intersection
                          #{"post" "posts"} %)
                       :org/tags))
         (map blog.item/note-row)
         (into [:div {:class "pl-4"}]))]

   [:hr]
   [:div
    [:h3 "Recently modified"]
    [:div
     (->>
       (notes-by-day (blog.db/published-notes))
       (take 5) ;; 5 most recent day blocks
       (map (fn [[day notes]] (day-block {:day day :notes notes})))
       (into [:div]))]]])

(comment
  (render/write-page
    {:path    "/index.html"
     :content (page)
     :title   "Home"}))
