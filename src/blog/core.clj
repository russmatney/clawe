(ns blog.core
  (:require
   [babashka.fs :as fs]
   [quickblog.api :as qb.api]
   [garden.db :as garden.db]
   [clojure.set :as set]
   [db.core :as db]
   [util :refer [ensure-uuid]]
   [clojure.string :as string]
   [org-crud.core :as org-crud]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-with-db-id [id]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?e
        :where
        [?e :org/source-file ?src-file]]
      id)
    ffirst))

(comment
  (db/query
    '[:find (pull ?e [*])
      :in $ ?e
      :where [?e _ _]]
    10402)
  (db/query
    '[:find (pull ?e [*])
      :where [?e :db/id 10124]]))

(defn fetch-with-org-id [id]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?id
        :where
        [?e :org/source-file ?src-file]
        [?e :org/id ?id]]
      (ensure-uuid id))
    ffirst))

(comment
  (->>
    (garden.db/fetch-db-garden-notes)
    (take 4))

  (fetch-with-org-id "b3c4eedb-336e-48be-a6b5-a570f0fc9eb3")
  (fetch-with-org-id #uuid "b3c4eedb-336e-48be-a6b5-a570f0fc9eb3")

  (db/query
    '[:find (pull ?e [*])
      :where [?e :org/id #uuid "b3c4eedb-336e-48be-a6b5-a570f0fc9eb3"]])

  (fetch-with-org-id "7e0158a6-3596-4dd9-8669-dce7c341bdac")
  (db/query
    '[:find (pull ?e [*])
      :where [?e :org/id #uuid "7e0158a6-3596-4dd9-8669-dce7c341bdac"]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; collecting posts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn included-posts []
  (->>
    (garden.db/notes-with-tags #{"post" "posts" "til"})
    (map :org/source-file)
    distinct
    ;; reparsing b/c the db fetching of children is odd rn
    (map org-crud/path->nested-item)))

(defn linked-db-ids [item]
  (let [from-children (->> item :org/items (mapcat linked-db-ids))]
    (concat (map
              :link/id
              (:org/links-to item))
            from-children)))

(defn linked-posts [posts]
  (->> posts
       (mapcat linked-db-ids)
       ;; TODO should be reparsing these as well? (vs. using the db obj)
       ;; these already get reparsed in path->html in quickblog, should minimize that
       ;; TODO should be connect to all linked nodes in the graph?
       ;; (this is just links one edge away)
       (map fetch-with-org-id)
       (remove nil?)))

(comment
  (->>
    (included-posts)
    (filter (comp #(string/includes? % "2022-09-11") :org/source-file))
    #_(filter (comp #(string/includes? % "upcoming") :org/name))
    #_(take 6)
    linked-posts
    #_count
    ))

(defn included-post-paths []
  (let [posts        (included-posts)
        linked-posts (linked-posts posts)]
    (->>
      (concat posts linked-posts)
      (map :org/source-file)
      distinct)))

(comment
  (count
    (included-post-paths)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; blog options
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def blog-root
  (str (fs/home) "/russmatney/new-blog"))

(defn blog-opts
  ([] (blog-opts blog-root))
  ([root]
   (let [paths (included-post-paths)]
     {:blog-description       "Clojure, Game Dev, and Nerd Tools"
      :blog-author            "Russell Matney"
      :post-paths             paths
      :num-index-posts        3
      :favicon                false
      :favicon-dir            (str root "/assets/favicon")
      :favicon-out-dir        (str root "/public/assets/favicon")
      #_#_:posts-dir          "_posts/techsposure"
      :assets-dir             (str root "/assets")
      :assets-out-dir         "public/assets"
      :cached-posts           {}
      :blog-url               "https://blog.russmatney.com"
      :blog-root              "https://github.com/russmatney/new-blog"
      :templates-dir          (str root "/templates")
      :out-dir                (str root "/public")
      :tags-dir               "tags"
      :default-metadata       {}
      :cache-dir              (str root "/.work")
      :blog-title             "Danger Russ Blog"
      :force-render           false
      :twitter-handle         "russmatney"
      :rendering-system-files #{"templates" "bb.edn" "deps.edn"}
      :posts-file             "posts.edn"

      ;; note that going through quickblog's dispatch clears items not in the api docs
      ;; so this would be cleared
      :keep-item
      (comp seq
            #(set/intersection #{"post" "posts" "til"} %)
            :org/tags)
      :remove-item
      (comp seq #(set/intersection #{"private"} %) :org/tags)
      :fetch-item fetch-with-org-id})))

(comment
  (blog-opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public blog api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render []
  (qb.api/render (blog-opts))
  nil)

(comment
  (render))

(defn watch
  ([] (watch nil))
  ([_]
   (qb.api/watch (assoc (blog-opts) :port 1999))))
