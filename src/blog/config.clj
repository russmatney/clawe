(ns blog.config
  (:require
   [taoensso.timbre :as log]
   [aero.core :as aero]
   [systemic.core :as sys :refer [defsys]]
   [babashka.fs :as fs]
   [zprint.core :as zp]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; blog content root
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn blog-content-root []
  (str (fs/home) "/russmatney/org-blog-content"))

(defn blog-content-public []
  (str (blog-content-root) "/public"))

(defn blog-content-config []
  (str (blog-content-root) "/blog.edn"))


(def blog-edn (blog-content-config))

(defn ->config [] (aero/read-config blog-edn))

(defsys ^:dynamic *config* :start
  (log/info "[BLOG-CONFIG]: Restarting *config*")
  (atom (->config)))

(defn reload-config []
  (sys/start! `*config*)
  (reset! *config* (->config)))

(defn write-config
  "Writes the current config to `resources/clawe.edn`"
  [updated-config]
  (sys/start! `*config*)
  (let [updated-config
        ;; note this is not a deep merge
        (merge @*config* updated-config)]
    (spit blog-edn (-> updated-config
                       (zp/zprint-str 100)
                       (string/replace "," "")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc config data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-google-analytics-id [] (:google-analytics-id @*config*))
(defn get-mastodon-href [] (:mastodon-href @*config*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; export-mode
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce !export-mode (atom nil))
(defn export-mode? [] @!export-mode)
(defn toggle-export-mode [] (swap! !export-mode not))
(defn set-export-mode [v] (reset! !export-mode v))

(comment
  (not nil)
  (export-mode?)
  (toggle-export-mode)
  (set-export-mode true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn note-defs
  "Returns a list of note defs.

  'defs' b/c this is not a full 'note', but just
  some persisted options related to a note."
  []
  (sys/start! `*config*)
  (->>
    (:notes @*config* {})
    (map (fn [[path def]]
           (assoc def :org/short-path path)))))

(defn note-def
  "Returns a single note def for the passed :org/short-path."
  [short-path]
  (sys/start! `*config*)
  ((:notes @*config* {}) short-path))

(defn persist-note-def
  "Adds the passed note to the :notes config.
  Expects at least :org/short-path on the config."
  ;; TODO consider including org/tags in the notes def, or otherwise building up a published-tags list
  [note]
  (let [short-path (:org/short-path note)]
    (if-not short-path
      (log/warn "[ERROR: config/update-note]: no :short-path for passed note")
      (-> @*config*
          (update-in [:notes short-path] merge note)
          write-config))
    (reload-config)))

(defn drop-note-def
  "Removes the note at the passed `:org/short-path` from the :notes config."
  [short-path]
  ;; TODO log warning when no note found to be dissoced
  (-> @*config*
      (update :notes dissoc short-path)
      write-config)
  (reload-config))

(comment
  (note-defs)
  (note-def "garden/some-note.org")
  (note-def "garden/games_journal.org")
  (persist-note-def {:org/short-path "garden/some-note.org"
                     :with/data      :hi/there})
  (drop-note-def "garden/some-note.org"))
