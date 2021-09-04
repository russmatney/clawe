(ns clawe.wallpapers
  (:require
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clawe.db.core :as db]
   [ralphie.notify :as notify]))


(defn local-wallpapers-file-paths []
  (->
    (zsh/expand "~/Dropbox/wallpapers/**/*")
    (string/split #" ")
    (->> (filter #(re-seq #"\.jpg$" %)))))

(defn get-wallpaper
  "Assumes the first is the one we want"
  [f]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?full-path
        :where
        [?e :file/full-path ?full-path]
        [?e :background/last-time-set ?t]]
      (:file/full-path f))
    ffirst))


(defn all-wallpapers
  "Baked in assumption that all wallpapers are one directory down."
  []
  (let [paths (local-wallpapers-file-paths)]
    (->>
      paths
      (map (fn [f]
             (let [common-path (str (-> f fs/parent fs/file-name) "/" (fs/file-name f))
                   initial-wp  {:file/full-path      f
                                :file/common-path    common-path
                                :file/web-asset-path (str "/assets/wallpapers/" (-> f fs/parent fs/file-name) "/" (fs/file-name f))
                                :name                (fs/file-name f)}
                   db-wp       (get-wallpaper initial-wp)]
               ;; db overwrites? or initial?
               (merge initial-wp db-wp)))))))

(comment
  (->>
    (all-wallpapers)
    (map :background/last-time-set)
    (remove nil?)
    )
  )


(defn set-wallpaper
  "Depends on `feh`."
  [f]
  (notify/notify "Setting wallpaper" f)
  (def --f f)
  (db/transact [(-> f
                    (assoc :background/last-time-set (System/currentTimeMillis))
                    (update :background/used-count (fnil inc 0)))])
  (-> (process/$ feh --bg-fill ~(:file/full-path f))
      process/check :out slurp))

(comment
  (get-wallpaper --f)
  (->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?full-path
        :where [?e :file/full-path ?full-path]]
      (:file/full-path --f))
    ffirst)

  (->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?full-path
        :where [?e :file/full-path ?full-path]]
      (:file/full-path --f)))

  )