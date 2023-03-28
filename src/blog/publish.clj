(ns blog.publish
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [taoensso.timbre :as log]
   [blog.render :as render]
   [blog.pages.daily :as pages.daily]
   [blog.pages.note :as pages.note]
   [blog.pages.last-modified :as pages.last-modified]
   [blog.pages.index :as pages.index]
   [blog.pages.tags :as pages.tags]
   [blog.db :as blog.db]
   [blog.config :as blog.config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_ "publish funcs"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish-note [path-or-note]
  (let [note
        (if (and
              (map? path-or-note)
              (:org/source-file path-or-note)
              (= :level/root (:org/level path-or-note))) path-or-note
            (some->>
              (blog.db/published-notes)
              (filter (comp #{(str path-or-note)} :org/source-file))
              first))]
    (if-not note
      (log/info "[PUBLISH] could not find note" path-or-note)
      (do
        (log/info "[PUBLISH] exporting note: " (:org/short-path note))
        (if (-> note :org/source-file (string/includes? "/daily/"))
          (render/write-page
            {:path    (str (blog.config/blog-content-public) (blog.db/note->uri note))
             :content (pages.daily/page note)
             :title   (:org/name-string note)})

          (render/write-page
            {:path    (str (blog.config/blog-content-public) (blog.db/note->uri note))
             :content (pages.note/page note)
             :title   (:org/name-string note)}))))))

(defn publish-notes []
  (let [notes-to-publish (blog.db/published-notes)]
    (doseq [note notes-to-publish]
      (publish-note note))))

(defn publish-index-by-tag []
  (log/info "[PUBLISH] exporting index-by-tag.")
  (render/write-page
    {:path    (str (blog.config/blog-content-public) "/tags.html")
     :content (pages.tags/page)
     :title   "Notes By Tag"}))

(defn publish-index-by-last-modified []
  (log/info "[PUBLISH] exporting index-by-last-modified.")
  (render/write-page
    {:path    (str (blog.config/blog-content-public) "/last-modified.html")
     :content (pages.last-modified/page)
     :title   "Notes By Modified Date"}))

(defn publish-index []
  (log/info "[PUBLISH] exporting index.")
  (render/write-page
    {:path    (str (blog.config/blog-content-public) "/index.html")
     :content (pages.index/page)
     :title   "Home"}))

(defn publish-indexes
  []
  (publish-index-by-tag)
  (publish-index-by-last-modified)
  (publish-index))

(defn publish-all
  ;; TODO delete notes/files that aren't here?
  []
  (let [start-t (t/now)]
    (publish-notes)
    (publish-indexes)
    (render/write-styles)
    (log/info "[PUBLISH]: blog publish complete"
              (str (t/millis (t/duration {:tick/beginning start-t
                                          :tick/end       (t/now)})) "ms"))))

(comment
  (publish-all))

(defn -main []
  (publish-all)
  (System/exit 0))
