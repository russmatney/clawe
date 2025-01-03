(ns doctor.ingest
  (:require
   [taoensso.telemere :as log]
   [babashka.fs :as fs]

   [garden.db :as garden.db]
   [clawe.config :as c.config]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn- ingest-file?
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
;; ingestors

(def garden-ingestor
  {:exts        #{"org"}
   :ingest-file garden.db/sync-and-purge-for-path
   :matches     {:daily  #"/todo/daily/"
                 :garden #"/todo/garden/"
                 :basic  #"/todo/icebox.org"}})

(def repo-todos-ingestor
  ;; TODO super broad! could get better treatment
  {:exts        #{"org"}
   :ingest-file garden.db/sync-and-purge-for-path
   :matches     {:todo #".org"}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn attempt-ingest [{:keys [path]}]
  (log/log! {:data {:path path}} "attempting ingest")

  ;; TODO break out 'garden' ingestor into blog-post, todos, garden-note, dev-log
  (->> [
        garden-ingestor
        repo-todos-ingestor
        ]
       (filter (fn [ingestor] (ingest-file? {:file path} ingestor)))
       (map (fn [{:keys [ingest-file]}]
              (log/log! {:data {:path path}} "ingesting file")
              (ingest-file path)))))
