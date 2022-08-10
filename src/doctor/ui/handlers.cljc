(ns doctor.ui.handlers
  (:require
   [plasma.core :refer [defhandler]]
   #?@(:clj [[garden.core :as garden]
             [ralphie.emacs :as emacs]
             [git.core :as git]
             [chess.core :as chess]
             [screenshots.core :as screenshots]
             [chess.db :as chess.db]
             [wallpapers.core :as wallpapers]
             [clawe.wm :as wm]]
       :cljs [])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler open-in-journal [item]
  (when-let [path (:org/source-file item)]
    (wm/show-client "journal")
    (emacs/open-in-emacs {:emacs/file-path  path
                          :emacs/frame-name "journal"}))
  :ok)

(comment
  (open-in-journal {:org/source-file "/home/russ/todo/journal.org"}))


(defhandler open-in-emacs [item]
  (when-let [path (:org/source-file item)]
    (emacs/open-in-emacs {:emacs/file-path path}))
  :ok)

(defhandler full-garden-items [paths]
  (->> paths
       (map garden/full-item)
       (into [])))

(defhandler full-garden-item [item]
  (garden/full-item item))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-screenshots []
  (screenshots/ingest-screenshots)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; lichess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-lichess-games []
  (chess.db/ingest-lichess-games)
  :ok)

(defhandler clear-lichess-games-cache []
  (chess/clear-cache)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-clawe-repos []
  (git/ingest-clawe-repos)
  :ok)

(defhandler ingest-commits-for-repo [repo]
  (println "ingesting commits for repo" repo)
  (git/ingest-commits-for-repo repo)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpapers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-wallpapers []
  (wallpapers/ingest-wallpapers)
  :ok)

(defhandler set-wallpaper [item]
  (wallpapers/set-wallpaper item)
  :ok)
