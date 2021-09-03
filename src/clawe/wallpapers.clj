(ns clawe.wallpapers
  (:require
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [babashka.fs :as fs]))


(defn local-wallpapers-files []
  (->
    (zsh/expand "~/Dropbox/wallpapers/**/*")
    (string/split #" ")
    (->> (filter #(re-seq #"\.jpg$" %)))))

(defn all-wallpapers []
  (let [paths (local-wallpapers-files)]
    (->>
      paths
      (map (fn [f]
             (def --f f)
             {:file/filename       f
              :file/web-asset-path (str "/assets/wallpapers/" (-> f fs/parent fs/file-name) "/" (fs/file-name f))
              :name                (fs/file-name f)})))))

(comment
  --f

  (-> --f fs/parent fs/file-name)
  )
