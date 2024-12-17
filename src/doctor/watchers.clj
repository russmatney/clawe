(ns doctor.watchers
  (:require
   [juxt.dirwatch :as dirwatch]
   [systemic.core :as sys :refer [defsys]]
   [taoensso.telemere :as log]
   [babashka.fs :as fs]

   [garden.db :as garden.db]
   [clawe.config :as c.config]
   [ralphie.config :as r.config]
   [api.screenshots :as api.screenshots]
   [api.clips :as api.clips]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn ingest-file?
  [{:keys [action file]}
   {:keys [skip-actions exts matches]}]
  (when-not (skip-actions action)
    (let [path (str file)]
      (and
        (not (string/includes? ".DS_Store" (str file)))
        (exts (fs/extension file))
        (->> matches
             (filter (fn [[k reg]]
                       (when (seq (re-seq reg path))
                         (log/log! :debug ["File matches pattern" k])
                         k)))
             first)))))

(defn on-file-event [event {:keys [ingest-file] :as rules}]
  (when (ingest-file? event rules)
    (log/log! {:data {:file (str (fs/file-name (:file event)))}} "Syncing file")
    (ingest-file (:file event))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden watcher

(def garden-rules
  {:exts         #{"org"}
   :skip-actions #{:delete}
   :ingest-file  garden.db/sync-and-purge-for-path
   :matches      {:daily  #"/todo/daily/"
                  :garden #"/todo/garden/"
                  :basic  #"/todo/icebox.org"}
   :->dir        c.config/garden-dir})

(defsys ^:dynamic *garden-watcher*
  :start
  (log/log! :info "Starting *garden-watcher*")
  (dirwatch/watch-dir #(on-file-event % garden-rules) ((:->dir garden-rules)))
  :stop
  (log/log! :debug "Closing *garden-watcher*")
  (dirwatch/close-watcher *garden-watcher*))

(comment
  (sys/start! `*garden-watcher*)
  *garden-watcher*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screenshot watcher

(def screenshot-rules
  {:exts         #{"png"}
   :skip-actions #{:delete}
   :ingest-file  api.screenshots/ingest-screenshot
   :matches      {:screenshots #"/Screenshots/"}
   :->dir        (fn [] (fs/file (r.config/screenshots-dir)))})

(defsys ^:dynamic *screenshot-watcher*
  :start
  (log/log! :info "Starting *screenshot-watcher*")
  (dirwatch/watch-dir #(on-file-event % screenshot-rules) ((:->dir screenshot-rules)))
  :stop
  (log/log! :debug "Closing *screenshot-watcher*")
  (dirwatch/close-watcher *screenshot-watcher*))

(comment
  (sys/start! `*screenshot-watcher*)
  *screenshot-watcher*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clip watcher

(def clip-rules
  {:exts         #{"png" "gif" "mp4" "jpg"}
   :skip-actions #{:delete}
   :ingest-file  api.clips/ingest-clip
   :matches      {:clips #"/game-clips/"}
   :->dir        (fn [] (fs/file (r.config/game-clips-dir)))})

(defsys ^:dynamic *clip-watcher*
  :start
  (log/log! :info "Starting *clip-watcher*")
  (dirwatch/watch-dir #(on-file-event % clip-rules) ((:->dir clip-rules)))
  :stop
  (log/log! :debug "Closing *clip-watcher*")
  (dirwatch/close-watcher *clip-watcher*))

(comment
  (sys/start! `*clip-watcher*)
  *clip-watcher*)
