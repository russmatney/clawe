^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.wallpapers
  (:require
   [wallpapers.core :as wallpapers]
   [nextjournal.clerk :as clerk]))

;; # wallpapers

(clerk/table
  (->>
    (wallpapers/all-wallpapers)
    (map (fn [{:keys [file/web-asset-path file/file-name]}]
           {:path web-asset-path
            :file file-name}))))
