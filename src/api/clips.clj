(ns api.clips
  (:require
   [clojure.string :as string]
   [babashka.fs :as fs]

   [dates.tick :as dates.tick]
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
    (let [time-string
          (-> fname
              (string/replace #"Kapture " "")
              (string/replace #" \d{2}%" "")
              (string/replace #" at " "_")
              (string/replace #" " "_")
              (string/replace #".gif" "")
              (string/replace #".mp4" ""))
          t (dates.tick/parse-time-string time-string)]

      {:file/full-path      f
       ;; NOTE this implies a symlink between the screenshots dir and the public assets dir
       :file/web-asset-path (str "/assets/clips/" fname)
       :name                fname
       :doctor/type         :type/clip
       :event/timestamp     t
       :clip/time           t
       :clip/time-string    time-string})))

(defn all-clips []
  (->> (local-clip-paths)
       (map fname->clip)
       (remove nil?)))

(defn ingest-clips []
  (->> (all-clips)
       (take 200)
       (db/transact)))

(comment
  (ingest-clips))
