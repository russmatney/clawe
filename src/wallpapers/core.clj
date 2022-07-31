(ns wallpapers.core
  (:require
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [defthing.db :as db]
   [ralphie.notify :as notify]
   [defthing.defwallpaper :as defwallpaper]))

(defn wp-dir->paths [root]
  (-> (zsh/expand root)
      (string/split #" /")
      (->> (map (fn [p]
                  (if (string/starts-with? p "/")
                    p
                    (str "/" p)))))))

(comment
  (zsh/expand "~/AbdelrhmanNile/onedark-wallpapers/onedark\\ wallpapers/*"))

(defn local-wallpapers-file-paths []
  (->
    (concat
      (wp-dir->paths "~/Dropbox/wallpapers/**/*")
      (wp-dir->paths "~/AbdelrhmanNile/onedark-wallpapers/onedark\\ wallpapers/*"))
    (->> (filter #(re-seq #"\.(jpg|png)$" %)))))

(comment
  (local-wallpapers-file-paths))

(defn all-wallpapers
  "Baked in assumption that all wallpapers are one directory down."
  []
  (let [paths (local-wallpapers-file-paths)]
    (->>
      paths
      (map (fn [f]
             (def --f f)
             (let [common-path (str (-> f fs/parent fs/file-name) "/" (fs/file-name f))
                   initial-wp  {:doctor/type          :type/wallpaper
                                :wallpaper/full-path  f
                                :wallpaper/short-path common-path
                                :file/web-asset-path  (str "/assets/wallpapers/"
                                                           (-> f fs/parent fs/file-name)
                                                           "/" (fs/file-name f))
                                :file/file-name       (fs/file-name f)}
                   db-wp       (defwallpaper/get-wallpaper initial-wp)]
               ;; db overwrites? or initial?
               (merge initial-wp db-wp)))))))


(comment
  (->>
    (all-wallpapers)
    (remove (comp nil? :wallpaper/last-time-set))))

(defn mark-wp-set
  ([w] (mark-wp-set w {}))
  ([w {:keys [skip-count]}]
   (when (= (:doctor/type w) :type/wallpaper)
     (db/transact (cond-> w
                    true
                    (assoc :wallpaper/last-time-set (System/currentTimeMillis))

                    (not skip-count)
                    (update :wallpaper/used-count (fnil inc 0)))))))

(defn set-wallpaper
  "Depends on `feh`."
  ([w] (set-wallpaper w {}))
  ([w opts]
   (notify/notify "Setting wallpaper" w)
   (mark-wp-set w opts)
   (-> (process/$ feh --bg-fill ~(:wallpaper/full-path w))
       process/check :out slurp)))

(comment
  ;; mark all as set once
  (->>
    (all-wallpapers)
    (map mark-wp-set))

  (defwallpaper/get-wallpaper --f)
  (->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?full-path
        :where [?e :wallpaper/full-path ?full-path]]
      (:wallpaper/full-path --f))
    ffirst)

  (->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?full-path
        :where [?e :wallpaper/full-path ?full-path]]
      (:wallpaper/full-path --f))))
