(ns doctor.ingest
  (:require
   [taoensso.telemere :as log]
   [babashka.fs :as fs]

   [garden.db :as garden.db]
   [clawe.config :as c.config]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn ingest-file?
  [{:keys [file]}
   {:keys [exts matches]}]
  (let [path (str file)]
    (and
      (not (string/includes? ".DS_Store" (str file)))
      (exts (fs/extension file))
      (->> matches
           (filter (fn [[k reg]]
                     (when (seq (re-seq reg path))
                       (log/log! :debug ["File matches pattern" k])
                       k)))
           first))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden watcher

(def garden-rules
  {:exts        #{"org"}
   :ingest-file garden.db/sync-and-purge-for-path
   :matches     {:daily  #"/todo/daily/"
                 :garden #"/todo/garden/"
                 :basic  #"/todo/icebox.org"}})

(def repo-todos-rules
  ;; TODO super broad! could get better treatment
  {:exts        #{"org"}
   :ingest-file garden.db/sync-and-purge-for-path
   :matches     {:todo #".org"}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn attempt-ingest [{:keys [path]}]
  (log/log! {:data {:path path}} "attempting ingest")

  ;; TODO break out 'garden' rules into blog-post, todos, garden-note, dev-log
  ;; could be multiple?

  (cond
    (ingest-file? {:file path} garden-rules)
    (do
      (log/log! {} "ingesting garden file")
      ((:ingest-file garden-rules) path))

    (ingest-file? {:file path} repo-todos-rules)
    (do
      (log/log! {} "ingesting repo todos file")
      ((:ingest-file repo-todos-rules) path))

    ))
