(ns doctor.api.wallpapers
  (:require
   [clawe.wallpapers :as c.wallpapers]))

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
      (into []))))

(defn reload
  "Reloads the current wallpaper. Falls back to the last-set wp."
  []
  (println "wallpapers-reload hit!")
  (c.wallpapers/set-wallpaper (last-used-wallpaper) {:skip-count true}))

(comment
  (def y (last-used-wallpaper))
  (def x (last-used-wallpaper)))
