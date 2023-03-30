(ns blog.config
  (:require
   [taoensso.timbre :as log]
   [aero.core :as aero]
   [tick.core :as t]
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
;; debug-mode
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce !debug-mode (atom true))
(defn debug-mode? [] @!debug-mode)
(defn toggle-debug-mode [] (swap! !debug-mode not))
(defn set-debug-mode [v] (reset! !debug-mode v))

(comment
  (not nil)
  (debug-mode?)
  (toggle-debug-mode)
  (set-debug-mode true))

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
  (count (note-defs))
  (note-def "garden/some-note.org")
  (note-def "garden/games_journal.org")
  (persist-note-def {:org/short-path "garden/some-note.org"
                     :with/data      :hi/there})
  (drop-note-def "garden/some-note.org")

  (t/date "2022-04-01")

  ;; parse git_all_versions_of output into :org/short-path -> earliest timestamp

  (let [all-parsed
        (->>
          (fs/list-dir (blog-content-root))
          (filter (fn [p]
                    (string/includes? (str p) "blog.edn.")))
          (map (fn [p]
                 (if (string/includes? (str p) "logmsg")
                   (let [msg-line  (->> p str slurp string/split-lines
                                        (filter #(re-seq #"Authored by" %))
                                        first)
                         timestamp (->> msg-line
                                        (re-seq #"Authored by Russell Matney at (.*)")
                                        first last
                                        (#(string/split % #" "))
                                        first
                                        t/date)
                         path      (fs/file-name p)]
                     [path timestamp])
                   (let [notes (-> p str aero/read-config :notes)
                         path  (fs/file-name p)]
                     [path notes])))))

        grouped
        (->> all-parsed
             (group-by (comp map? second)))
        notes-by-commit     (get grouped true)
        timestamp-by-commit (into {} (get grouped false))

        notes-with-timestamps
        (->> notes-by-commit
             (mapcat (fn [[commit notes]]
                       (->> notes keys
                            (map (fn [short-path]
                                   [short-path commit])))))
             (group-by first)
             (map (fn [[k v]]
                    [k (->> v (map second) sort last
                            (#(str % ".logmsg"))
                            timestamp-by-commit)])))

        commit-dates->note-paths
        (->> notes-with-timestamps
             (group-by second)
             (map (fn [[k v]]
                    [k (map first v)]))
             (into {}))]
    ;; timestamp-by-commit
    ;; notes-with-timestamps
    #_(->> commit-dates->note-paths
           vals
           (apply concat)
           count)
    (->>
      commit-dates->note-paths
      (map (fn [[date paths]]
             (->> paths
                  (map (fn [p]
                         (persist-note-def {:org/short-path    p
                                            :blog/published-at date})))
                  doall)))
      doall))
  )
