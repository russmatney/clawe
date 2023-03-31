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

(defn list-of-links [{:keys [links header subheader]}]
  [:div
   [:h3 header]
   (when subheader
     (cond
       (string? subheader) [:p subheader]
       :else               subheader))
   (->> links
        (into [:div]))])

(defn page []
  [:div

   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h2 {:class ["font-nes"]} "Home"]]

   (->
     (blog.db/find-note "blog_home.org")
     (blog.item/item->hiccup-content {:skip-title true}))

   [:hr]

   [:div
    {:class ["flex flex-col"]}
    [:div
     {:class ["flex flex-row" "justify-center"]}
     [:h2 "Find notes..."]]
    [:div
     {:class ["flex flex-row"
              "space-x-4"
              "justify-between"]}
     [:p
      {:class ["not-prose"]}
      [:a {:href  "/tags.html"
           :class ["text-city-pink-400"
                   "hover:text-city-pink-200"
                   "font-nes"]}
       "by tag"
       ;; TODO include 5 most common tags
       ]]
     [:p
      {:class ["not-prose"]}
      [:a {:href  "/last-modified.html"
           :class ["text-city-green-400"
                   "hover:text-city-green-200"
                   "font-nes"]}
       "by last modified date"]]
     [:p
      {:class ["not-prose"]}
      [:a {:href  "/tags.html#index"
           :class ["text-city-blue-400"
                   "hover:text-city-blue-200"
                   "font-nes"]}
       "via indexes"
       ;; TODO include 5 most common tags
       ]]
     ]]

   [:hr]

   (list-of-links
     {:header "Dino Games and Addons"
      :subheader
      [:p
       "I'm building a suite of games in "
       [:span
        {:class ["not-prose"]}
        [:a {:href  "https://github.com/russmatney/dino"
             :class ["text-city-red-400"
                     "hover:text-city-red-200"
                     "font-mono" "font-bold"]}
         "Dino"]]
       (str ", a godot sandbox and monorepo.")]
      :links
      (->> (blog.db/published-notes)
           (filter (comp seq
                         (fn [tags]
                           (and
                             (seq (set/intersection #{"dino" "dinogame" "dinoaddon"} tags))
                             (seq (set/intersection #{"project" "game" "addon"} tags))))
                         :org/tags))
           (map blog.item/note-row))})

   [:hr]

   (list-of-links
     {:header "Clojure Projects"
      :subheader
      [:p
       [:span
        {:class ["not-prose"]}
        [:a {:href  "https://github.com/russmatney/clawe"
             :class ["text-city-red-400"
                     "hover:text-city-red-200"
                     "font-mono" "font-bold"]}
         "Clawe"]]
       " is my clojure monorepo, and it's absorbed most of the projects here already."]
      :links
      (->> (blog.db/published-notes)
           (filter (comp
                     (fn [tags]
                       (and
                         (or
                           (seq (set/intersection #{"clojure"} tags))
                           (seq (set/intersection #{"orgcrud"} tags))
                           (seq (set/intersection #{"clawe"} tags)))
                         (seq (set/intersection #{"project"} tags))))
                     :org/tags))
           (map blog.item/note-row))})

   [:hr]

   (list-of-links
     {:header "Posts"
      :links
      (->> (blog.db/published-notes)
           (filter (comp
                     (fn [tags]
                       (and
                         (not (seq (set/intersection #{"draft"} tags)))
                         (seq (set/intersection #{"post" "posts"} tags))))
                     :org/tags))
           (map blog.item/note-row))})

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
