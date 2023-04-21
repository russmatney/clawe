(ns api.blog
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [tick.core :as t]

   [blog.config :as blog.config]
   [blog.db :as blog.db]
   [blog.publish :as blog.publish]
   [blog.render :as blog.render]
   [dates.tick :as dates]
   [ralphie.browser :as browser]
   [org-crud.update :as org-crud.update]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; infra
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-blog-data []
  (let [db        (blog.db/get-db)
        month-ago (t/<< (dates/now) (t/new-period 2 :months))]
    {:root-notes
     (->> db :root-notes-by-id vals
          (filter (fn [note]
                    (when-not (some-> note :file/last-modified)
                      (log/info "Note without :file/last-modified" note))
                    (when (some-> note :file/last-modified)
                      (dates/sort-latest-first
                        (-> note :file/last-modified dates/parse-time-string)
                        month-ago))))
          (sort-by :org/name-string))}))

(defsys ^:dynamic *blog-data-stream*
  :start (s/stream)
  :stop (s/close! *blog-data-stream*))

(defn update-blog-data []
  (log/info "Pushing blog data update to client")
  (s/put! *blog-data-stream* (build-blog-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish/unpublish
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish [note]
  (log/info "Publishing" (:org/short-path note))

  (let [;; update note
        note (-> note
                 (assoc :blog/published-at (t/date))
                 (update :org/tags conj "published"))]

    ;; add "published" tag to note without editing last-modified
    (org-crud.update/update!
      note {:org.update/reset-last-modified true
            :org/tags                       (:org/tags note)})

    ;; add note to blog.edn config (and update blog.config state)
    (blog.config/persist-note-def
      (-> note (select-keys [:org/short-path :org/name-string])))

    ;; update blog.db state
    (blog.db/update-db-note note))

  ;; push update to client
  (update-blog-data))

(defn unpublish [note]
  (log/info "Unpublishing" (:org/short-path note))
  (blog.config/drop-note-def (:org/short-path note))
  (blog.db/update-db-note note)
  (update-blog-data))

(defn rebuild-all []
  (blog.publish/publish-all))

(defn rebuild-indexes
  "Also rebuilds blog styles."
  []
  (blog.publish/publish-indexes)
  (blog.render/write-styles))

(defn rebuild-open-pages []
  ;; TODO impl proper tab support on osx
  (browser/tabs)
  ;; TODO impl
  ;; collect open pages via ralphie.browser
  ;; call publish-note with those notes
  #_(blog.publish/publish-indexes))

(defn restart-systems []
  (blog.config/reload-config)
  (blog.db/refresh-notes))
