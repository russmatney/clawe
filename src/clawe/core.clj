(ns clawe.core
  (:require
   [ralphie.install :as install]
   [ralph.defcom :as defcom :refer [defcom]]
   [ralphie.sh :as r.sh])
  )

(defcom hello
  {:name    "hello"
   :handler (fn [_config _parsed]
              (println "Howdy!"))})

;; *file*
;; io/file
;; .getParent
;; (str "/" ~fname)

(defcom install
  {:name "install"
   :handler
   (fn [_config _parsed]
     (println "Symlinking repo/awesome to your ~/.config/awesome")
     (install/symlink
       (r.sh/expand "~/russmatney/clawe/awesome")
       ;; TODO use ~WDG_CONFIG~ or whatever that thing is
       (r.sh/expand "~/.config/awesome")))})

(defn -main [& args]
  (apply defcom/run args))
