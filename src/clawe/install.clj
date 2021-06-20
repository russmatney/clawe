(ns clawe.install
  (:require
   [babashka.process :refer [$ check]]
   [clojure.java.shell :as sh :refer [with-sh-dir]]

   [ralphie.install :as r.install]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [ralphie.config :as r.config]
   [ralphie.rofi :as r.rofi]

   [ralph.defcom :as defcom :refer [defcom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install Awesome config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom install-cmd
  {:defcom/name "install"
   :defcom/handler
   (fn [_config _parsed]
     (println "Symlinking repo/awesome to your ~/.config/awesome")
     (r.install/symlink
       (r.sh/expand "~/russmatney/clawe/awesome")
       ;; TODO use ~WDG_CONFIG~ or whatever that thing is
       (r.sh/expand "~/.config/awesome")))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build Clawe Uberjar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-uberscript []
  (let [proc "rebuilding-clawe-uberscript"]
    (r.notify/notify {:subject          "Clawe Uberscript: Rebuilding"
                      :replaces-process proc})
    (let [cp (r.util/get-cp (r.sh/expand "~/russmatney/clawe"))]
      (->
        ^{:dir (r.sh/expand "~/russmatney/clawe")}
        ($ bb -cp ~cp -m clawe.core --uberscript clawe-script.clj)
        check)
      (r.notify/notify {:subject          "Clawe Uberscript: Rebuild Complete"
                        :replaces-process proc}))))

(defn build-uberjar []
  (let [proc "rebuilding-clawe-uberjar"]
    (r.notify/notify {:subject          "Clawe Uberjar: Rebuilding"
                      :replaces-process proc})
    (let [cp (r.util/get-cp (r.sh/expand "~/russmatney/clawe"))]
      (->
        ^{:dir (r.sh/expand "~/russmatney/clawe")}
        ($ bb -cp ~cp --uberjar clawe.jar -m clawe.core )
        check)
      (r.notify/notify {:subject          "Clawe Uberjar: Rebuild Complete"
                        :replaces-process proc}))))

(defcom build-clawe
  {:defcom/name    "rebuild-clawe"
   :defcom/handler (fn [_config _parsed]
                     (build-uberjar)
                     ;; (build-uberscript)
                     )})
