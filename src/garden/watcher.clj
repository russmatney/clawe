(ns garden.watcher
  (:require
   [juxt.dirwatch :as dirwatch]
   [systemic.core :as sys :refer [defsys]]
   [ralphie.zsh :as zsh]
   [taoensso.timbre :as log]
   [babashka.fs :as fs]
   [garden.db :as garden.db]))

(defn garden-dir-path []
  (fs/file (zsh/expand "~/todo")))

(def should-sync-match-strs
  {:daily     #"/todo/daily/"
   :archive   #"/todo/archive/"
   :workspace #"/todo/garden/workspaces/"
   :garden    #"/todo/garden/"
   :basic     #"/todo/[journal|projects|icebox].org"})

(defn should-sync-file? [file]
  (let [path (str file)]
    (and (#{"org"} (fs/extension file))
         (->> should-sync-match-strs
              (filter (fn [[k reg]]
                        (when (seq (re-seq reg path))
                          (log/debug "File matches pattern" k)
                          k)))
              first))))


(defsys *garden-watcher*
  :start
  (log/info "Starting *garden-watcher*")
  (dirwatch/watch-dir
    (fn [event]
      (log/debug (:action event) "event" event)
      (when (and (not (#{:delete} (:action event)))
                 (should-sync-file? (:file event)))
        (log/debug "Syncing file" (str (fs/file-name (:file event))))
        (garden.db/sync-garden-paths-to-db
          {:page-size 200} ;; decent page size, fewer transactions
          [(:file event)])))
    (garden-dir-path))

  :stop
  (log/debug "Closing *garden-watcher*")
  (dirwatch/close-watcher *garden-watcher*))


(comment
  (sys/start! `*garden-watcher*)
  *garden-watcher*
  )
