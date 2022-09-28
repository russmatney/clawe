^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.wallpapers
  (:require
   [wallpapers.core :as wallpapers]
   [nextjournal.clerk :as clerk]
   [notebooks.nav :as nav]))

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer     nav/nav-viewer}
nav/nav-options

;; # wallpapers

(clerk/table
  (->>
    (wallpapers/all-wallpapers)
    (map (fn [{:keys [file/web-asset-path file/file-name]}]
           {:path web-asset-path
            :file file-name}))))
