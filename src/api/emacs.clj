(ns api.emacs
  (:require [ralphie.emacs :as emacs]))

(comment
  (println "hi"))

(defn open-in-emacs [item]
  (def --item item)
  (println "open in emacs" item))
