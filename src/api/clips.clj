(ns api.clips
  (:require
   [babashka.fs :as fs]
   [taoensso.telemere :as log]

   [dates.tick :as dates]
   [db.core :as db]
   [ralphie.config :as r.config]))

(defn local-clip-paths []
  (let [dir (r.config/game-clips-dir)]
    (if (not (fs/exists? dir))
      (println "clip dir path does not exist!" dir)
      (->> dir
           fs/list-dir
           (filter fs/directory?)
           (#(fs/list-dirs % "*.{gif,mp4}"))
           (map str)))))

(comment
  (local-clip-paths))


(defn fname->clip [f]
  (when-let [fname (fs/file-name f)]
    (let [time        (str (fs/creation-time f))
          t           (dates/parse-time-string time)
          parent-name (-> f fs/parent fs/file-name)
          ]
      {:file/full-path      f
       ;; TODO support this properly for clips (need parent game-dir)
       :file/web-asset-path (str "/assets/game-clips/"
                                 parent-name "/" fname)
       :name                fname
       :doctor/type         :type/clip
       :event/timestamp     t
       :clip/time           t})))

(defn all-clips []
  (->> (local-clip-paths)
       (map fname->clip)
       (remove nil?)))

(defn ingest-clips []
  (log/log! {} "Ingesting game clips")
  (->> (all-clips)
       (sort-by :event/timestamp dates/sort-latest-first)
       (take 200)
       (db/transact)))

(comment
  (ingest-clips))

(defn ingest-clip [fname]
  (->> fname fname->clip (db/transact)))

(comment
  (->>
    (all-clips)
    (sort-by :event/timestamp dates/sort-latest-first)
    (take 1)
    )
  )
