(ns defthing.defwallpaper
  (:require
   [ralphie.zsh :as zsh]
   [defthing.db :as db]))


(comment
  (zsh/expand "~/AbdelrhmanNile/onedark-wallpapers/onedark\\ wallpapers/*"))

(defn get-wallpaper
  "Assumes the first is the one we want"
  [f]
  (some->>
    (db/query
      '[:find [(pull ?e [*])]
        :in $ ?full-path
        :where
        [?e :doctor/type :type/wallpaper]
        [?e :wallpaper/full-path ?full-path]
        [?e :wallpaper/last-time-set ?t]]
      (:wallpaper/full-path f))
    first))

(comment
  (defwallpaper/get-wallpaper --f)
  )


(defn all-wallpapers []
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :doctor/type :type/wallpaper]])
    (map first)))

(comment
  (all-wallpapers)

  6
  )
