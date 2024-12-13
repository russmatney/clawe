(ns blog.publish
  (:require
   [clojure.string :as string]
   [babashka.fs :as fs]
   [tick.core :as t]
   [taoensso.telemere :as log]

   [dates.tick :as dates]
   [ralphie.notify :as notify]
   [blog.render :as render]
   [blog.pages.daily :as pages.daily]
   [blog.pages.note :as pages.note]
   [blog.pages.date-index :as pages.date-index]
   [blog.pages.index :as pages.index]
   [blog.pages.tags :as pages.tags]
   [blog.pages.resources :as pages.resources]
   [blog.db :as blog.db]
   [blog.config :as blog.config]
   [components.note :as note]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish notes
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
      (log/log! :info ["[PUBLISH] could not find published note" path-or-note])
      (do
        (log/log! :info ["[PUBLISH] exporting note: " (:org/short-path note)])
        (render/write-page
          {:note  note
           :content
           (if (-> note :org/source-file (string/includes? "/daily/"))
             (pages.daily/page note)
             (pages.note/page note))
           :title (:org/name-string note)})))))

(defn publish-notes []
  (let [notes-to-publish (blog.db/published-notes)]
    (log/log! :info ["[PUBLISH] publishing " (count notes-to-publish) " notes"])
    (doseq [note notes-to-publish]
      (publish-note note))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish indexes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish-index-by-tag []
  (log/log! :info "[PUBLISH] exporting index-by-tag.")
  (render/write-page
    {:path    "/tags.html"
     :content (pages.tags/page)
     :title   "Notes By Tag"}))

(defn publish-date-indexes []
  (log/log! :info "[PUBLISH] exporting date-indexes.")

  (render/write-page
    {:path    "/created-at.html"
     :content (pages.date-index/page {:note->date :org.prop/created-at
                                      :uri        "/created-at.html"})
     :title   "By Date Created"})

  (render/write-page
    {:path    "/published-at.html"
     :content (pages.date-index/page {:note->date :blog/published-at
                                      :uri        "/published-at.html"})
     :title   "By Date Published"})

  (render/write-page
    {:path    "/last-modified.html"
     :content (pages.date-index/page {:uri        "/last-modified.html"
                                      :note->date :file/last-modified})
     :title   "By Modified Date"}))

(defn publish-index []
  (log/log! :info "[PUBLISH] exporting index.")
  (render/write-page
    {:path    "/index.html"
     :content (pages.index/page)
     :title   "Home"}))

(defn publish-indexes
  []
  (publish-index-by-tag)
  (publish-date-indexes)
  (publish-index)
  (pages.resources/publish))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish images
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ensure-image-dir []
  (let [dir (blog.config/blog-content-images)]
    (when-not (fs/exists? dir)
      (log/log! :info "creating image dir")
      (fs/create-dirs dir))))

(defn publish-images []
  (ensure-image-dir)
  (let [notes  (blog.db/published-notes)
        images (->> notes
                    ;; TODO exclude unpublished daily items
                    (mapcat note/->all-images))]
    (log/log! :info ["[PUBLISH] exporting" (count images) "images"])
    (->> images
         (map (fn [{:as img :keys [image/path]}]
                (let [path ;; TODO attempt expand-home in org-crud
                      (fs/expand-home path)]
                  (if (fs/exists? path)
                    (let [new-path (blog.config/image->blog-path img)]
                      (log/log! :debug ["[PUBLISH] copying image" new-path])
                      (fs/copy path new-path {:replace-existing true}))
                    (log/log! :warn ["[PUBLISH] image path does not exist" path])))))
         doall
         (remove nil?)
         count
         (#(log/log! :info ["[PUBLISH] Copied" % "images"])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish devlogs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ensure-devlogs-dir []
  (let [dir (blog.config/blog-content-devlogs)]
    (when-not (fs/exists? dir)
      (log/log! :info "creating devlog dir")
      (fs/create-dirs dir))))

(defn publish-devlogs []
  (ensure-devlogs-dir)
  (let [paths (blog.config/devlog-html-paths)]
    (log/log! :info ["[PUBLISH] exporting" (count paths) "devlog html files"])
    (->> paths (map (fn [path]
                      (let [new-path (blog.config/path->devlog-path path)]
                        (log/log! :debug ["[PUBLISH] copying path" new-path])
                        (fs/copy path new-path {:replace-existing true}))))
         doall
         (remove nil?)
         count
         (#(log/log! :info ["[PUBLISH] Copied" % "images"])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish-all
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish-all
  ;; TODO delete notes/files that aren't here?
  []
  (let [start-t  (t/now)
        notif-id "publishing-blog"]
    (notify/notify {:subject "Rebuilding blog..."
                    :body    "Building all notes and indexes"
                    :id      notif-id})
    (blog.db/refresh-notes)
    ;; images have to go first, so that notes hide/show divs for existing images
    (publish-images)
    (publish-notes)
    (publish-indexes)
    (publish-devlogs)
    (render/write-styles)
    (let [time-str (str (dates/millis-since start-t) "ms")]
      (log/log! :info ["[PUBLISH]: blog publish complete " time-str])
      (notify/notify {:subject "Rebuilding blog [Complete]"
                      :body    (str "Rebuilt in " time-str)
                      :id      notif-id}))))

(comment
  (publish-all)
  (publish-note (blog.db/find-note "clove.org"))
  (publish-note (blog.db/find-note "beehive.org")))

(defn -main []
  (publish-all)
  (System/exit 0))
