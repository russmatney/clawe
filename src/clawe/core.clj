(ns clawe.core
  (:require
   [clawe.workspaces]
   [babashka.process :refer [$ check]]

   [ralphie.install :as r.install]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]

   [ralph.defcom :as defcom :refer [defcom]]
   [clawe.awesome :as awm]))

(defcom hello-cmd
  {:name    "hello"
   :handler (fn [_config _parsed]
              (println "Howdy"))})

(defcom install-cmd
  {:name "install"
   :handler
   (fn [_config _parsed]
     (println "Symlinking repo/awesome to your ~/.config/awesome")
     (r.install/symlink
       (r.sh/expand "~/russmatney/clawe/awesome")
       ;; TODO use ~WDG_CONFIG~ or whatever that thing is
       (r.sh/expand "~/.config/awesome")))})

(defn build-uberscript []
  (r.notify/notify "Re-Building Clawe Uberscript")
  (let [cp (r.util/get-cp (r.sh/expand "~/russmatney/clawe"))]
    (->
      ^{:dir (r.sh/expand "~/russmatney/clawe")}
      ($ bb -cp ~cp -m clawe.core --uberscript clawe-script.clj)
      check)
    (r.notify/notify "Clawe Uberscript Rebuilt.")))

;; (defn install-uberjar
;;   "Writes and chmods a shell script directly to .local"
;;   []
;;   (spit "/home/russ/.local/bin/clawe"
;;         (str "#!/bin/sh
;; exec bb /home/russ/russmatney/clawe/clawe-script.clj $@"))
;;   (r.sh/sh "chmod" "+x" "/home/russ/.local/bin/clawe-script"))

(defn build-clawe-uberscript []
  (build-uberscript)
  ;; (install-uberjar)
  ;; (r.install/symlink
  ;;   (r.sh/expand "~/russmatney/clawe/clawe-script.clj")
  ;;   "/home/russ/.local/bin/clawe-script")
  )

(defcom build-clawe
  {:name    "rebuild-clawe"
   :handler (fn [_config _parsed] (build-clawe-uberscript))})

(defcom awm-cli-cmd
  {:name     "awm-cli"
   ;; this doesn't do anything, but felt easier than documentation....
   :validate (fn [arguments] (-> arguments first string?))
   :handler
   (fn [_config {:keys [arguments]}]
     (awm/awm-cli (first arguments)))})

(defn -main [& args]
  (apply defcom/run args))
