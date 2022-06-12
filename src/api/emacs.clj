(ns api.emacs
  (:require [ralphie.emacs :as emacs]))

(defn open-in-emacs [item]
  (when-let [file-path (some item [:org/source-file])]
    (emacs/open-in-emacs {:emacs/file-path file-path})))
