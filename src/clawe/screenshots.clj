(ns clawe.screenshots
  (:require [babashka.process :as proc]
            [ralphie.zsh :as zsh]
            [clojure.string :as string]
            [babashka.fs :as fs]))


(defn local-screenshot-file-paths []
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


(defn fname->screenshot [f]
  (let [fname (fs/file-name f)]
    {:file/full-path         f
     :file/web-asset-path    (str "/assets/screenshots/" fname)
     :name                   fname
     :screenshot/time-string (-> fname
                                 (string/replace #"screenshot_" "")
                                 (string/replace #".jpg" ""))}))


(defn all-screenshots []
  (->> (local-screenshot-file-paths)
       (map fname->screenshot)))
