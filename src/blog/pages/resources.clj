(ns blog.pages.resources
  (:require
   [clojure.string :as string]

   [blog.item :as item]
   [blog.db :as blog.db]
   [blog.render :as render]
   [db.core :as db]
   [garden.db :as garden.db]
   [org-crud.core :as org-crud]))

;; could consider pulling _every_ url, rather than focusing on this 'resources' tag + org/name

(defn resource-notes []
  (->>
    (db/query
      '[:find (pull ?note [*])
        :where
        [?note :doctor/type :type/note]
        [?note :org/name-string ?name]
        (or
          [?note :org/tags "resources"]
          [(clojure.string/includes? ?name "resources")])])
    (map first)
    (garden.db/join-children)
    (map (fn [db-note]
           (let [found-note (some-> db-note :org/id blog.db/id->note)]
             (if found-note
               (merge db-note found-note)
               ;; TODO support looking up note in blog.db without :org/id
               ;; probably similarly via :org/fallback-id
               db-note))))))

(defn resource-notes-from-blog-db []
  (->>
    (blog.db/all-notes)
    (filter (fn [note]
              (or
                (-> note :org/name-string (string/includes? "resources"))
                (-> note :org/tags (#(% "resources"))))))))

(comment
  (count (resource-notes))
  (count (resource-notes-from-blog-db))

  (->>
    (resource-notes)
    (filter (comp seq :org/items)))

  (->>
    #_(resource-notes)
    (resource-notes-from-blog-db)
    first
    org-crud/nested-item->flattened-items)

  (->>
    (db/query
      '[:find (pull ?note [*])
        :where
        [?note :doctor/type :type/note]
        [?note :org/parent-name ?pname]
        [(clojure.string/includes? ?pname "mac osx customization")]
        [?note :org/name-string ?name]
        [(clojure.string/includes? ?name "resources")]])
    (map first)
    (garden.db/join-children))

  (db/query '[:find (pull ?c [*])
              :in $ ?db-id
              :where [?c :org/parents ?db-id]]
            19429))

(defn url-block [note]
  (let [flat (org-crud/nested-item->flattened-items note)
        urls (->> flat (mapcat :org/urls) distinct)]
    (when (seq urls)
      [:div
       {:class ["flex" "flex-col"]}
       [:h3 (:org/parent-name note)]
       (item/item->hiccup-headline note)
       (->> urls
            (map (fn [url]
                   [:li (item/->hiccup-link {:link url})]))
            (into [:ul
                   {:class ["flex" "flex-col"
                            "p-4"]}]))
       [:hr
        {:class ["text-slate-200"]}]])))

(comment
  (->> (resource-notes-from-blog-db) first url-block))


;; could group-by :org/urls to show multiple contexts with same link
;; could group domain: github, youtube, reddit, etc
;; probably fairly well grouped by topic already
(defn page []
  [:div
   [:div
    {:class ["flex" "flex-col" "justify-center"]}
    [:h2 {:class ["font-mono"]} "Notes By Tag"]]
   (->>
     (resource-notes-from-blog-db)
     (map url-block)
     (remove nil?)
     (into [:div]))])

(defn publish []
  (render/write-page
    {:path    "/resources.html"
     :content (page)
     :title   "Resources"}))

(comment
  (publish))
