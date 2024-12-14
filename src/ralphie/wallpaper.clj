(ns ralphie.wallpaper
  (:require
   [babashka.process :as process]

   [ralphie.config :as config]))


(defn set-wallpaper [{:keys [path]}]
  (if (config/osx?)
    (->
      (process/$ osascript -e
                 ~(str
                    "tell application \"System Events\" to tell every desktop to set picture to \""
                    path
                    "\""))
      process/check :out slurp)
    (-> (process/$ feh --bg-fill ~path)
        process/check :out slurp)))
