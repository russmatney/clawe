(ns clawe.screenshots
  (:require [babashka.process :as proc]
            [ralphie.zsh :as zsh]
            [clojure.string :as string]


            [babashka.fs :as fs]))


(defn local-screenshot-files []
  (let [base-dir (zsh/expand "~/Screenshots/")]
    (->
      ^{:dir base-dir :out :string}
      (proc/$ ls)
      proc/check :out
      (string/split #"\n")
      (->>
        (filter seq)
        (map #(str base-dir %))
        sort
        ;; attempting to cheaply sort by date
        ;; could just renamed the ones in the wrong pattern to match the new ones
        reverse))))

(defn all-screenshots []
  (let [filenames (local-screenshot-files)]
    (->>
      filenames
      (take 5)
      (map (fn [f]
             {:file/filename       f
              :file/web-asset-path (str "/assets/screenshots/" (fs/file-name f))
              :name                (fs/file-name f)})))))
