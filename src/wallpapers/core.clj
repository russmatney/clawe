(ns wallpapers.core
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as string]

   [ralphie.zsh :as zsh]
   [ralphie.notify :as notify]
   [db.core :as db]))

(defn wp-dir->paths [root]
  (-> (zsh/expand root)
      (string/split #" /")
      (->> (map (fn [p]
                  (if (string/starts-with? p "/")
                    p
                    (str "/" p)))))))

(defn local-wallpapers-file-paths []
  (->
    (wp-dir->paths "~/Dropbox/wallpapers/**/*")
    (->> (filter #(re-seq #"\.(jpg|png)$" %)))))

(comment
  (local-wallpapers-file-paths))

(defn build-db-wallpapers
  "Baked in assumption that all wallpapers are one directory down."
  []
  (let [paths (local-wallpapers-file-paths)]
    (->>
      paths
      (map (fn [f]
             (let [common-path (str (-> f fs/parent fs/file-name) "/" (fs/file-name f))]
               {:doctor/type          :type/wallpaper
                :file/full-path       f
                :wallpaper/short-path common-path
                :file/web-asset-path  (str "/assets/wallpapers/"
                                           (-> f fs/parent fs/file-name)
                                           "/" (fs/file-name f))
                :file/file-name       (fs/file-name f)}))))))

(defn ingest-wallpapers []
  (->> (build-db-wallpapers)
       (db/transact)))

(defn all-wallpapers []
  (->>
    (db/query '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/wallpaper]])
    (map first)))

(comment
  (->>
    (all-wallpapers)
    (group-by :file/web-asset-path)
    (filter (comp #(> % 1) count second))
    (map second)
    (map first)
    (map :db/id)
    (db/retract)
    ;; (filter (comp nil? :file/full-path))
    ;; (map :db/id)
    ;; (map db/retract)
    ))

(defn mark-wp-set
  ([w] (mark-wp-set w {}))
  ([w {:keys [skip-count]}]
   (when (#{:type/wallpaper} (:doctor/type w))
     (db/transact (cond-> w
                    true
                    (assoc :wallpaper/last-time-set (System/currentTimeMillis))

                    (not skip-count)
                    (update :wallpaper/used-count (fnil inc 0)))))))


(defn set-wallpaper
  "Depends on `feh`."
  ([w] (set-wallpaper nil w))
  ([opts w]
   (notify/notify "Setting wallpaper" w)
   (mark-wp-set w opts)
   (-> (process/$ feh --bg-fill ~(:file/full-path w))
       process/check :out slurp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn last-used-wallpaper []
  (->>
    (all-wallpapers)
    (filter :wallpaper/last-time-set)
    (sort-by :wallpaper/last-time-set)
    reverse
    first))

(defn reload
  "Reloads the current wallpaper. Falls back to the last-set wp."
  []
  (println "wallpapers-reload hit!")
  (when-let [wp (last-used-wallpaper)]
    (set-wallpaper {:skip-count true} wp)))
