(ns doctor.ui.handlers
  (:require
   [plasma.core :refer [defhandler]]
   #?@(:clj [[ralphie.emacs :as emacs]]
       :cljs [])))

(defhandler open-in-emacs [item]
  (when-let [path (:org/source-file item)]
    (emacs/open-in-emacs {:emacs/file-path path}))
  :ok)
