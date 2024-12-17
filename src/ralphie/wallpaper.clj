(ns ralphie.wallpaper
  (:require
   [babashka.process :as process]
   [clojure.string :as string]

   [ralphie.zsh :as zsh]
   [ralphie.config :as r.config]))

(defn wp-dir->paths [root]
  (-> (zsh/expand root)
      (string/split #" /")
      (->> (map (fn [p]
                  (if (string/starts-with? p "/")
                    p (str "/" p)))))))

(defn wallpaper-file-paths []
  (->
    (wp-dir->paths (str (r.config/wallpapers-dir) "/**/*"))
    (concat (wp-dir->paths (str (r.config/wallpapers-dir) "/*")))
    (->> (filter #(re-seq #"\.(jpeg|jpg|png)$" %)))))

(comment
  (wallpaper-file-paths))

(defn set-wallpaper [{:keys [path]}]
  (println "ralphie setting wallpaper" path)
  (if (r.config/osx?)
    (->
      (process/$ osascript -e
                 ~(str
                    "tell application \"System Events\" to tell every desktop to set picture to \""
                    path
                    "\""))
      process/check :out slurp)
    (-> (process/$ feh --bg-fill ~path)
        process/check :out slurp)))
