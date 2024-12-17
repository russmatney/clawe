(ns clawe.config
  (:require
   [aero.core :as aero]
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [systemic.core :as sys :refer [defsys]]
   ;; [taoensso.telemere :as log]
   [zprint.core :as zp]

   [ralphie.config :as r.config]
   [timer :as timer]))

(timer/print-since "clawe.config ns loading")


(def clawe-config-path ".config/clawe/clawe.edn")
(def clawe-local-config-path ".local/share/clawe/local.edn")
(def clawe-logs-path ".log/clawe/clawe.out.txt")

(defn config-resource [path]
  (let [conf-clawe (io/file (str (fs/home) "/" path))]
    (if (fs/exists? conf-clawe) conf-clawe
        (do
          (println :error [path "not found, falling back on template."])
          (io/resource "clawe-template.edn")))))

(defn read-config [path]
  (aero/read-config (config-resource path)))

(defn ->config []
  (let [conf (->
               (read-config clawe-config-path)
               (assoc :is-mac (r.config/osx?))
               (assoc :home-dir (str (fs/home)))
               (->> ;; local should overwrite global
                 (merge (read-config clawe-local-config-path))))]
    (timer/print-since "parsed and returning clawe config")
    conf))

(defsys ^:dynamic *config*
  :start
  (timer/print-since "starting clawe.config system")
  (atom (->config)))

(declare write-config)

(defn reload-config []
  (if (sys/running? `*config*)
    ;; restart the system to force a re-read from the file
    (sys/restart! `*config*)
    (sys/start! `*config*))

  ;; write out the config to force the formatting
  (write-config nil))

(comment
  (sys/restart! `*config*)
  @*config*
  (reload-config))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; write config to file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def do-not-write-global-keys #{:is-mac :home-dir :wm})

(defn write-config
  "Writes the current config to `resources/clawe.edn`"
  ([updated-config] (write-config updated-config nil))
  ([updated-config opts]
   (sys/start! `*config*)
   (let [path             (:path opts clawe-config-path)
         writeable-config (if (:is-local? opts false)
                            ;; read and merge into the local file
                            (-> (read-config clawe-local-config-path)
                                (merge updated-config))
                            ;; merge with *config*, filter computed keys
                            (-> @*config*
                                (merge updated-config)
                                (#(apply dissoc % do-not-write-global-keys))))]
     (spit (config-resource path)
           (-> writeable-config
               (zp/zprint-str 100)
               (string/replace "," ""))))))

(comment
  (write-config nil)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-dir []
  (sys/start! `*config*)
  (:home-dir @*config*))

(defn doctor-base-url []
  (sys/start! `*config*)
  (:doctor-base-url @*config*))

(defn repo-roots []
  (sys/start! `*config*)
  (:repo-roots @*config* []))

(defn local-dev-urls []
  (sys/start! `*config*)
  (:local-dev-urls @*config* []))

(defn common-urls []
  (sys/start! `*config*)
  (:common-urls @*config* []))

(defn garden-dir []
  (sys/start! `*config*)
  (fs/file (str (fs/home) "/" (:garden-dir @*config*))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; set/get current window manager
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-wm
  ([] (get-wm nil))
  ([_opts]
   (sys/start! `*config*)
   (:wm @*config*)))

(defn set-wm [{:keys [wm]}]
  {:org.babashka/cli {}}
  (sys/start! `*config*)
  (let [val        (cond
                     (#{"i3" :i3 :wm/i3} wm)                :wm/i3
                     (#{"awesome" :awesome :wm/awesome} wm) :wm/awesome
                     (#{"yabai" :yabai :wm/yabai} wm)       :wm/yabai)
        current-wm (get-wm)]
    (when-not val
      (println :warn ["Unhandled set-wm val" wm]))

    (when-not (= val current-wm)
      (println :warn "New window-manager, writing local config")
      (-> {:wm val} (write-config
                      {:path      clawe-local-config-path
                       :is-local? true}))))
  (reload-config)
  (get-wm))

(comment
  (set-wm {:wm "yabai"})
  (get-wm))

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

