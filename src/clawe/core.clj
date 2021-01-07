(ns clawe.core
  (:require
   [ralphie.install :as install]
   [ralph.defcom :as defcom :refer [defcom]]
   [ralphie.sh :as r.sh]
   [clawe.awesome :as awm]))

(defcom hello-cmd
  {:name    "hello"
   :handler (fn [_config _parsed]
              (println "Howdy!"))})

(defcom install-cmd
  {:name "install"
   :handler
   (fn [_config _parsed]
     (println "Symlinking repo/awesome to your ~/.config/awesome")
     (install/symlink
       (r.sh/expand "~/russmatney/clawe/awesome")
       ;; TODO use ~WDG_CONFIG~ or whatever that thing is
       (r.sh/expand "~/.config/awesome")))})

(defcom awm-cli-cmd
  {:name     "awm-cli"
   ;; this doesn't do anything, but felt easier than documentation....
   :validate (fn [arguments] (-> arguments first string?))
   :handler
   (fn [_config {:keys [arguments]}]
     (awm/awm-cli (first arguments)))})

(defn -main [& args]
  (apply defcom/run args))
