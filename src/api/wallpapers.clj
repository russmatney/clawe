(ns api.wallpapers
  (:require
   [clawe.wallpapers :as c.wallpapers]
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]))

(defn last-used-wallpaper []
  (->>
    (c.wallpapers/all-wallpapers)
    (filter :background/last-time-set)
    (sort-by :background/last-time-set)
    reverse
    first))

(defn active-wallpapers []
  (let [all (c.wallpapers/all-wallpapers)]
    (->>
      all
      (sort-by :background/last-time-set)
      reverse
      (take 30)
      (into []))))

(defn reload
  "Reloads the current wallpaper. Falls back to the last-set wp."
  []
  (println "wallpapers-reload hit!")
  (c.wallpapers/set-wallpaper (last-used-wallpaper) {:skip-count true}))

(comment
  (def y (last-used-wallpaper))
  (def x (last-used-wallpaper)))

(defsys *wallpapers-stream*
  :start (s/stream)
  :stop (s/close! *wallpapers-stream*))

(defn update-wallpapers []
  (println "pushing to wallpapers stream (updating wallpapers)!")
  (s/put! *wallpapers-stream* (active-wallpapers)))
