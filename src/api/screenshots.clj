(ns api.screenshots
  (:require
   [clojure.string :as string]
   [babashka.fs :as fs]

   [ralphie.screenshot :as r.screenshot]
   [dates.tick :as dates.tick]
   [db.core :as db]))


(defn fname->screenshot [f]
  (when-let [fname (fs/file-name f)]
    (let [time-string
          (-> fname
              (string/replace #"screenshot_" "")
              (string/replace #"Screen Shot " "")
              (string/replace #"Screenshot " "")
              (string/replace #"_\d{2,4}x\d{2,4}_scrot_000" "")
              (string/replace #"_\d{2,4}x\d{2,4}_scrot_001" "")
              (string/replace #"_\d{2,4}x\d{2,4}_scrot_002" "")
              (string/replace #"_\d{2,4}x\d{2,4}_scrot" "")
              (string/replace #" at " "_")
              (string/replace #".png" "")
              (string/replace #".jpg" ""))]
      {:file/full-path         f
       ;; NOTE this implies a symlink between the screenshots dir and the public assets dir
       :file/web-asset-path    (str "/assets/screenshots/" fname)
       :name                   fname
       :doctor/type            :type/screenshot
       :screenshot/time        (dates.tick/parse-time-string time-string)
       :screenshot/time-string time-string})))


(defn all-screenshots []
  (->> (r.screenshot/screenshot-file-paths)
       (map fname->screenshot)
       (remove nil?)))

(defn ingest-screenshots []
  (->> (all-screenshots)
       (take 200)
       (db/transact)))
