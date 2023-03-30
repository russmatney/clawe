(ns api.blog
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [manifold.stream :as s]
   [tick.core :as t]

   [blog.config :as blog.config]
   [blog.db :as blog.db]
   [blog.publish :as blog.publish]
   [dates.tick :as dates]))

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
  (blog.config/persist-note-def
    (-> note
        (select-keys [:org/short-path :org/name-string])
        (assoc :blog/published-at (t/date))))
  (blog.db/update-db-note note)
  (update-blog-data))

(defn unpublish [note]
  (log/info "Unpublishing" (:org/short-path note))
  (blog.config/drop-note-def (:org/short-path note))
  (blog.db/update-db-note note)
  (update-blog-data))

(defn republish-all []
  (blog.publish/publish-all))
