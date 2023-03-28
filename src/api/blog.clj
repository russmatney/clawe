(ns api.blog
  (:require
   [blog.db :as blog.db]
   [blog.config :as blog.config]
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [clojure.string :as string]
   [dates.tick :as dates]
   [tick.core :as t]))

(comment
  (blog.config/reload-config)
  (blog.config/note-defs)

  ;; WARN also kills nrepl/all systems!
  (blog.db/refresh-notes))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; infra
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-blog-data []
  (let [db        (blog.db/get-db)
        month-ago (t/<< (dates/now) (t/new-period 2 :months))]
    {:root-notes (->> db :root-notes-by-id
                      vals
                      (filter (fn [note]
                                (t/> (-> note :file/last-modified dates/parse-time-string)
                                     month-ago)))
                      (sort-by :org/name-string))}))

(comment
  (count
    (:root-notes (build-blog-data))))

(defsys ^:dynamic *blog-data-stream*
  :start (s/stream)
  :stop (s/close! *blog-data-stream*))

(comment
  (sys/start! `*blog-data-stream*))

(defn update-blog-data []
  (log/info "Pushing blog data update to client")
  (s/put! *blog-data-stream* (build-blog-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; publish/unpublish
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn publish [note]
  (log/info "Publishing" (:org/short-path note))
  (blog.config/persist-note-def
    (-> note (select-keys [:org/short-path
                           :org/name-string])))
  (blog.db/update-db-note note)
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
  (blog.db/update-db-note note)
  (update-blog-data))
