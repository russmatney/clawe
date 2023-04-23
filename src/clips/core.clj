(ns clips.core
  (:require
   [babashka.process :as proc]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [dates.tick :as dates.tick]
   [db.core :as db]))


(defn local-file-paths []
  (let [base-dir (str (fs/home) "/gifs/")]
    (->
      ^{:dir base-dir :out :string}
      (proc/$ ls)
      proc/check :out
      (string/split #"\n"))))


(defn fname->clip [f]
  (when-let [fname (fs/file-name f)]
    (let [time-string
          (-> fname
              (string/replace #"Kapture " "")
              (string/replace #" \d{2}%" "")
              (string/replace #" at " "_")
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
  (->> (local-file-paths)
       (map fname->clip)
       (remove nil?)))

(defn ingest-clips []
  (->> (all-clips)
       (take 200)
       (db/transact)))
