(ns garden.watcher
  (:require
   [juxt.dirwatch :as dirwatch]
   [systemic.core :as sys :refer [defsys]]
   [taoensso.telemere :as log]
   [babashka.fs :as fs]

   [garden.db :as garden.db]))

(defn garden-dir-path []
  (fs/file (str (fs/home) "/todo")))

(def should-sync-match-strs
  {:daily  #"/todo/daily/"
   :garden #"/todo/garden/"
   :basic  #"/todo/icebox.org"})

(defn should-sync-file? [file]
  (let [path (str file)]
    (and (#{"org"} (fs/extension file))
         (->> should-sync-match-strs
              (filter (fn [[k reg]]
                        (when (seq (re-seq reg path))
                          (log/log! :debug ["File matches pattern" k])
                          k)))
              first))))

(defsys ^:dynamic *garden-watcher*
  :start
  (log/log! :info "Starting *garden-watcher*")
  (dirwatch/watch-dir
    (fn [event]
      ;; TODO ignore .git
      ;; (when-not (#{".git"} (some-> event :file str fs/extension))
      ;;   (log/log! :debug [(:action event) "event" event]))
      (when (and (not (#{:delete} (:action event)))
                 (should-sync-file? (:file event)))
        (log/log! {:level :info
                   :data  {:file (str (fs/file-name (:file event)))}}
                  "Syncing file")
        (garden.db/sync-and-purge-for-path (:file event))))
    (garden-dir-path))

  :stop
  (log/log! :debug "Closing *garden-watcher*")
  (dirwatch/close-watcher *garden-watcher*))


(comment
  (sys/start! `*garden-watcher*)
  *garden-watcher*)
