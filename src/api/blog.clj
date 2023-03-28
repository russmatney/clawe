(ns api.blog
  (:require
   [blog.db :as blog.db]
   [blog.config :as blog.config]
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [clojure.string :as string]
   ))

(comment
  (blog.config/reload-config)
  (blog.config/note-defs)

  ;; WARN also kills nrepl/all systems!
  (blog.db/refresh-notes)

  (blog.db/notes-by-short-path)
  (blog.db/notes-by-id))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; infra
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-blog-data []
  (blog.db/get-db))

(defsys ^:dynamic *blog-data-stream*
  :start (s/stream)
  :stop (s/close! *blog-data-stream*))

(comment
  (sys/start! `*blog-data-stream*))

(defn update-blog-data []
  (blog.db/refresh-notes)
  (s/put! *blog-data-stream* (build-blog-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish/unpublish
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish [note]
  (log/info "Publishing" (:org/short-path note))
  (blog.config/persist-note-def
    (-> note (select-keys [:org/short-path
                           :org/name-string])))
  (update-blog-data))

(comment
  (select-keys {:a 1 :b 2} [:b])

  (blog.db/refresh-notes)

  (->>
    (build-blog-data)
    :all-notes
    (filter (fn [note]
              (string/includes? (:org/short-path note) "games_journal")))))

(defn unpublish [note]
  (log/info "Unpublishing" (:org/short-path note))
  (blog.config/drop-note-def (:org/short-path note))
  (update-blog-data))
