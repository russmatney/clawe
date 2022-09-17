(ns blog.core
  (:require
   [babashka.fs :as fs]
   [quickblog.api :as qb.api]))


(defn post-paths []
  (->>
    (concat
      (fs/glob (str (fs/home) "/todo/garden/") "*.org")
      (fs/glob (str (fs/home) "/todo/daily/") "*.org"))
    (sort-by fs/last-modified-time)
    reverse
    (take 10)))

(comment
  (->>
    (post-paths)
    (map (fn [v]
           (str (fs/last-modified-time v) " - " (str v))))
    #_(map str)))


(def blog-root
  (str (fs/home) "/russmatney/new-blog"))

(defn blog-opts
  ([] (blog-opts blog-root))
  ([root]
   {:blog-description       "Clojure, Game Dev, and Nerd Tools"
    :blog-author            "Russell Matney"
    :post-paths             (post-paths)
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
    :posts-file             "posts.edn"}))

(comment
  (blog-opts))


(defn render []
  (qb.api/render (blog-opts)))

(defn watch
  ([] (watch nil))
  ([_]
   (qb.api/watch (assoc (blog-opts) :port 1999))))
