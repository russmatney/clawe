(ns clawe.defs.apps
  (:require
   [babashka.process :refer [$ check]]
   [clawe.defthing :as defthing]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apps API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-apps []
  (defthing/list-xs :clawe/apps))

(defn get-app [app]
  (defthing/get-x :clawe/apps
    (comp #{(some app [:name identity])} :name)))

(defmacro defapp [title & args]
  (apply defthing/defthing :clawe/apps title args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE this is not used, just an idea for now
(defapp spotify-app
  {:defcom/handler (fn [_ _] (-> ($ spotify) check))
   :defcom/name    "start-spotify-client"})
