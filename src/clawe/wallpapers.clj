(ns clawe.wallpapers
  (:require
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clawe.db.core :as db]
   [ralphie.notify :as notify]))

(defn wp-dir->paths [root]
  (-> (zsh/expand root)
      (string/split #" /")
      (->> (map #(str "/" %)))))

(comment
  (zsh/expand "~/AbdelrhmanNile/onedark-wallpapers/onedark\\ wallpapers/*"))

(defn local-wallpapers-file-paths []
  (->
    (concat
      (wp-dir->paths "~/Dropbox/wallpapers/**/*")
      ;; (wp-dir->paths "~/AbdelrhmanNile/onedark-wallpapers/onedark\\ wallpapers/*")
      )
    (->> (filter #(re-seq #"\.(jpg|png)$" %)))))

(comment
  (local-wallpapers-file-paths))

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
                                :file/web-asset-path (str "/assets/wallpapers/"
                                                          (-> f fs/parent fs/file-name)
                                                          "/" (fs/file-name f))
                                :name                (fs/file-name f)}
                   db-wp       (get-wallpaper initial-wp)]
               ;; db overwrites? or initial?
               (merge initial-wp db-wp)))))))

(comment
  (->>
    (all-wallpapers)
    (map :background/last-time-set)
    (remove nil?)))

(defn set-wallpaper
  "Depends on `feh`."
  ([w] (set-wallpaper w {}))
  ([w {:keys [skip-count]}]
   (notify/notify "Setting wallpaper" w)
   (db/transact [(cond-> w
                   true
                   (assoc :background/last-time-set (System/currentTimeMillis))

                   (not skip-count)
                   (update :background/used-count (fnil inc 0)))])
   (-> (process/$ feh --bg-fill ~(:file/full-path w))
       process/check :out slurp)))

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
      (:file/full-path --f))))
