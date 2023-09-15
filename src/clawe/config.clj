(ns clawe.config
  (:require
   [taoensso.timbre :as log]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as zsh]
   ;; [ralphie.emacs :as emacs]
   [babashka.fs :as fs]
   [systemic.core :as sys :refer [defsys]]
   [zprint.core :as zp]
   [clojure.string :as string]

   [timer :as timer]
   ))

(timer/print-since "clawe.config ns loading")

(defn calc-is-mac? []
  (boolean (string/includes? (zsh/expand "$OSTYPE") "darwin")))

(def clawe-config-path ".config/clawe/clawe.edn")

(defn config-res []
  (let [conf-clawe (io/file (str (fs/home) "/" clawe-config-path))]
    (if (fs/exists? conf-clawe) conf-clawe
        (do
          ;; TODO add check for clawe.edn to doctor check-in
          (log/error "clawe.edn not found, falling back on template.")
          (io/resource "clawe-template.edn")))))

(defn ->config []
  (let [conf
        (->
          (aero/read-config (config-res))
          (assoc :is-mac (calc-is-mac?))
          (assoc :home-dir (str (fs/home))))]
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
    (spit (config-res) (-> writable-config
                           (zp/zprint-str 100)
                           (string/replace "," "")))
    ;; emacsclient -e '(progn (find-file "~/.config/clawe/clawe.edn") (aggressive-indent-indent-region-and-on (point-min) (point-max)) (save-buffer))'
    ;; TODO may need to 'force' the save? or wait to avoid a race-case?
    ;; TODO prevent the file from opening, and the save from prompting for confirmation, etc
    ;;(ralphie.emacs/fire "(progn (find-file \"~/.config/clawe/clawe.edn\") (aggressive-indent-indent-region-and-on (point-min) (point-max)) (save-buffer))")
    ))

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

(defn local-dev-urls []
  (sys/start! `*config*)
  (:local-dev-urls @*config* []))

(defn common-urls []
  (sys/start! `*config*)
  (:common-urls @*config* []))

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
      (log/warn "Unhandled set-wm val" wm))

    (when-not (= val current-wm)
      (log/warn "New window-manager, writing config")
      (-> @*config* (assoc :wm val) write-config)))
  (reload-config)
  (get-wm))

(comment
  (set-wm {:wm "i3"})
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
