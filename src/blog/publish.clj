(ns blog.publish
  (:require
   [clojure.string :as string]
   [tick.core :as t]
   [taoensso.timbre :as log]
   [dates.tick :as dates]
   [ralphie.notify :as notify]
   [blog.render :as render]
   [blog.pages.daily :as pages.daily]
   [blog.pages.note :as pages.note]
   [blog.pages.last-modified :as pages.last-modified]
   [blog.pages.index :as pages.index]
   [blog.pages.tags :as pages.tags]
   [blog.db :as blog.db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_ "publish funcs"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish-note [path-or-note]
  (let [note
        (if (and (map? path-or-note)
                 (:org/source-file path-or-note)
                 (= :level/root (:org/level path-or-note))
                 (blog.db/published-id? (:org/id path-or-note)))
          path-or-note
          (some->>
            (blog.db/published-notes)
            (filter (comp #{(str path-or-note)} :org/source-file))
            first))]
    (if-not note
      (log/info "[PUBLISH] could not find published note" path-or-note)
      (do
        (log/info "[PUBLISH] exporting note: " (:org/short-path note))
        (render/write-page
          {:note  note
           :content
           (if (-> note :org/source-file (string/includes? "/daily/"))
             (pages.daily/page note)
             (pages.note/page note))
           :title (:org/name-string note)})))))

(defn publish-notes []
  (let [notes-to-publish (blog.db/published-notes)]
    (log/info "[PUBLISH] publishing " (count notes-to-publish) " notes")
    (doseq [note notes-to-publish]
      (publish-note note))))

(defn publish-index-by-tag []
  (log/info "[PUBLISH] exporting index-by-tag.")
  (render/write-page
    {:path    "/tags.html"
     :content (pages.tags/page)
     :title   "Notes By Tag"}))

(defn publish-index-by-last-modified []
  (log/info "[PUBLISH] exporting index-by-last-modified.")
  (render/write-page
    {:path    "/last-modified.html"
     :content (pages.last-modified/page)
     :title   "Notes By Modified Date"}))

(defn publish-index []
  (log/info "[PUBLISH] exporting index.")
  (render/write-page
    {:path    "/index.html"
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
  (let [start-t  (t/now)
        notif-id "publishing-blog"]
    (notify/notify {:subject "Rebuilding blog..."
                    :body    "Building all notes and indexes"
                    :id      notif-id})
    (publish-notes)
    (publish-indexes)
    (render/write-styles)
    (let [time-str (str (dates/millis-since start-t) "ms")]
      (log/info "[PUBLISH]: blog publish complete " time-str)
      (notify/notify {:subject "Rebuilding blog [Complete]"
                      :body    (str "Rebuilt in " time-str)
                      :id      notif-id}))))

(comment
  (publish-all)

  (publish-note
    (blog.db/find-note "clove.org"))
  )

(defn -main []
  (publish-all)
  (System/exit 0))
