(ns doctor.ui.handlers
  (:require
   [plasma.core :refer [defhandler]]
   #?@(:clj [[garden.core :as garden]
             [ralphie.emacs :as emacs]]
       :cljs [])))

(defhandler open-in-emacs [item]
  (when-let [path (:org/source-file item)]
    (emacs/open-in-emacs {:emacs/file-path path}))
  :ok)

(defhandler full-garden-items [paths]
  (println "fetching items for paths" paths)
  (->> paths
       (map garden/full-item)
       (into [])))

(defhandler full-garden-item [path]
  (garden/full-item path))
