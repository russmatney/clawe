(ns api.blog
  (:require
   [blog.db :as blog.db]
   [blog.config :as blog.config]

   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]))

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
  (s/put! *blog-data-stream* (build-blog-data)))
