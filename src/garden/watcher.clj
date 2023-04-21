(ns garden.watcher
  (:require
   [juxt.dirwatch :as dirwatch]
   [systemic.core :as sys :refer [defsys]]
   [taoensso.timbre :as log]
   [babashka.fs :as fs]
   [garden.db :as garden.db]
   [api.todos :as api.todos]
   [clojure.string :as string]))

(defn ->repo-root [repo-id]
  (str (fs/home) "/" repo-id))

;; TODO support repo todo/readme.orgs in garden watcher
(defn repo-todo-files []
  ;; TODO pull roots from clawe workspace config
  (let [repo-roots ["russmatney/dino"
                    "russmatney/org-crud"
                    "russmatney/clawe"
                    "teknql/fabb"]]
    (->> repo-roots
         (map ->repo-root)
         (mapcat (fn [root-path]
                   [(str root-path "/todo.org")
                    (str root-path "/readme.org")
                    ;; one day, source todos from code files
                    ]))
         (filter fs/exists?))))



(defn garden-dir-path []
  (fs/file (str (fs/home) "/todo")))

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

(defn should-push-updated-data? [file]
  (or
    (string/includes? (str file) "daily")
    (string/includes? (str file) "garden")))

(defsys ^:dynamic *garden-watcher*
  :start
  (log/info "Starting *garden-watcher*")
  (dirwatch/watch-dir
    (fn [event]
      (log/debug (:action event) "event" event)
      (when (and (not (#{:delete} (:action event)))
                 (should-sync-file? (:file event)))
        (log/debug "Syncing file" (str (fs/file-name (:file event))))
        (garden.db/sync-and-purge-for-path (:file event)))

      (when (should-push-updated-data? (:file event))
        (log/debug "Pushing updated todos" (str (fs/file-name (:file event))))
        (api.todos/push-todos)))
    (garden-dir-path))

  :stop
  (log/debug "Closing *garden-watcher*")
  (dirwatch/close-watcher *garden-watcher*))


(comment
  (sys/start! `*garden-watcher*)
  *garden-watcher*
  )
