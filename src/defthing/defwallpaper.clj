(ns defthing.defwallpaper
  (:require
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [babashka.process :as process]
   [defthing.db :as db]
   [ralphie.notify :as notify]))


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
        [?e :file/full-path ?full-path]
        [?e :background/last-time-set ?t]]
      (:file/full-path f))
    first))

(comment

  (defwallpaper/get-wallpaper --f)
  )
