(ns clawe.config
  (:require
   [clojure.pprint :as pprint]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as zsh]
   [systemic.core :as sys :refer [defsys]]))

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

(defsys *config*
  :start
  (atom (->config)))

(defn reload-config []
  (sys/start! `*config*)
  (reset! *config* (->config)))

(comment
  (sys/start! `*config*)
  (reload-config))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; write config to file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def do-not-write-keys #{:is-mac :home-dir})

(defn write-config
  "Writes the current config to `resources/clawe.edn`"
  [updated-config]
  (sys/start! `*config*)
  (let [updated-config  (merge @*config* updated-config)
        writable-config (apply dissoc updated-config do-not-write-keys)]
    (pprint/pprint writable-config (io/writer res))))

(comment
  (write-config nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-dir []
  (sys/start! `*config*)
  (:home-dir @*config*))

(defn doctor-base-url []
  (sys/start! `*config*)
  (:doctor-base-url @*config*))

(defn is-mac? []
  (sys/start! `*config*)
  (:is-mac @*config*))

(defn repo-roots []
  (sys/start! `*config*)
  (:repo-roots @*config* []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace-defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-defs []
  (sys/start! `*config*)
  (:workspace/defs @*config* {}))

(defn workspace-defs-with-titles []
  (sys/start! `*config*)
  (->> (:workspace/defs @*config* {})
       (map (fn [[k def]]
              [k (assoc def :workspace/title k)]))
       (into {})))

(defn workspace-def [workspace-title]
  ((workspace-defs) workspace-title))

(defn update-workspace-def [workspace-title def]
  (sys/start! `*config*)
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
  (sys/start! `*config*)
  (->> (:client/defs @*config* {})
       (map (fn [[key def]]
              (assoc def :client/key key)))))

(defn client-def [client-key]
  (sys/start! `*config*)
  ((:client/defs @*config* {}) client-key))

(defn update-client-def [client-key def]
  (sys/start! `*config*)
  (-> @*config*
      (update-in [:client/defs client-key] merge def)
      write-config)
  (reload-config))

(comment
  (update-client-def "journal" {:client/some "other-data"}))
