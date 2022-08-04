(ns clawe.config
  (:require
   [clojure.pprint :as pprint]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as zsh]))

(defn calc-is-mac? []
  (boolean (#{"darwin21"} (zsh/expand "$OSTYPE"))))

(def res (io/resource "clawe.edn"))

(defn ->config []
  (->
    (aero/read-config res)
    (assoc :is-mac (calc-is-mac?))
    (assoc :home-dir (zsh/expand "~"))))

(comment
  (->config))

(defonce ^:dynamic *config* (atom (->config)))

(defn reload-config []
  (reset! *config* (->config)))

(comment
  (reload-config))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; write config to file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def do-not-write-keys #{:is-mac :home-dir})

(defn write-config
  "Writes the current config to `resources/clawe.edn`"
  [updated-config]
  (let [updated-config  (merge @*config* updated-config)
        writable-config (apply dissoc updated-config do-not-write-keys)]
    (pprint/pprint writable-config (io/writer res))))

(comment
  (write-config nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-dir []
  (:home-dir @*config*))

(defn doctor-base-url []
  (:doctor-base-url @*config*))

(defn is-mac? []
  (:is-mac @*config*))

(defn repo-roots []
  (:repo-roots @*config* []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace-defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-defs []
  (:workspace/defs @*config* {}))

(defn workspace-def [workspace-title]
  ((workspace-defs) workspace-title))

(defn update-workspace-def [workspace-title def]
  (-> @*config*
      (update-in [:workspace/defs workspace-title] merge def)
      write-config)
  (reload-config))

(comment
  (update-workspace-def "journal" {:workspace/some "other-data"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client-defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-defs []
  (->> (:client/defs @*config* {})
       (map (fn [[key def]]
              (assoc def :client/key key)))))

(defn update-client-def [client-key def]
  (-> @*config*
      (update-in [:client/defs client-key] merge def)
      write-config)
  (reload-config))

(comment
  (update-client-def "journal" {:client/some "other-data"}))
