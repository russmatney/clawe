(ns api.wallpapers
  (:require
   [wallpapers.core :as wallpapers]
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]))

(defn last-used-wallpaper []
  (->>
    (wallpapers/all-wallpapers)
    (filter :wallpaper/last-time-set)
    (sort-by :wallpaper/last-time-set)
    reverse
    first))

(defn active-wallpapers []
  (let [all (wallpapers/all-wallpapers)]
    (->>
      all
      (sort-by :wallpaper/last-time-set)
      reverse
      (take 30)
      (into []))))

(defn reload
  "Reloads the current wallpaper. Falls back to the last-set wp."
  []
  (println "wallpapers-reload hit!")
  (wallpapers/set-wallpaper (last-used-wallpaper) {:skip-count true}))

(defsys *wallpapers-stream*
  :start (s/stream)
  :stop (s/close! *wallpapers-stream*))

(defn update-wallpapers []
  (println "pushing to wallpapers stream (updating wallpapers)!")
  (s/put! *wallpapers-stream* (active-wallpapers)))
