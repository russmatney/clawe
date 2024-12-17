(ns api.wallpapers
  (:require
   [babashka.fs :as fs]
   [taoensso.telemere :as log]

   [ralphie.notify :as notify]
   [ralphie.wallpaper :as r.wallpaper]
   [db.core :as db]))


(defn build-db-wallpapers
  "Baked in assumption that all wallpapers are one directory down."
  []
  (let [paths (r.wallpaper/wallpaper-file-paths)]
    (->>
      paths
      (map (fn [f]
             (let [common-path (str (-> f fs/parent fs/file-name) "/" (fs/file-name f))]
               {:doctor/type          :type/wallpaper
                :file/full-path       f
                :wallpaper/short-path common-path
                :file/web-asset-path
                (str "/assets/wallpapers/"
                     (let [parent-name (-> f fs/parent fs/file-name)]
                       (if (= "wallpapers" parent-name) ""
                           (str parent-name "/")))
                     (fs/file-name f))
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
    ;; (filter (comp #(> % 1) count second))
    (map second)
    (map first)
    (map :db/id)
    (db/retract-entities)
    ;; (filter (comp nil? :file/full-path))
    ;; (map :db/id)
    ;; (map db/retract-entites)
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


;; TODO move this to ralphie
(defn set-wallpaper
  "Depends on `feh`."
  ([w] (set-wallpaper nil w))
  ([opts w]
   (notify/notify "Setting wallpaper" w)
   (log/log! {:data w} "setting wallpaper")
   (mark-wp-set w opts)
   (r.wallpaper/set-wallpaper {:path (:file/full-path w)})))

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
  "Reloads the last-used wallpaper. Falls back to the last-set wp."
  []
  (println "wallpapers-reload hit!")
  (when-let [wp (last-used-wallpaper)]
    (set-wallpaper {:skip-count true} wp)))
