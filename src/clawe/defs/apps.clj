(ns clawe.defs.apps
  (:require
   [babashka.process :refer [$ check]]
   [defthing.core :as defthing]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apps API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-apps []
  (defthing/list-things :clawe/apps))

(defn get-app [app]
  (defthing/get-thing :clawe/apps
    (comp #{(:name app app)} :name)))

(defmacro defapp [title & args]
  (apply defthing/defthing :clawe/apps title args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE this is not used, just an idea for now
(defapp spotify-app
  {:defcom/handler (fn [_ _] (-> ($ spotify) check))
   :defcom/name    "start-spotify-client"})
