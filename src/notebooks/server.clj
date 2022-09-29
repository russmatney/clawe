^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.server
  (:require
   [nextjournal.clerk :as clerk]
   [babashka.fs :as fs]))

(defn ->f [path]
  {:path path
   :name (some-> path
                 fs/file-name
                 fs/split-ext
                 first)})

(comment
  (->f *file*)
  (some-> *file* fs/file-name fs/split-ext first))

^{::clerk/visibility {:code :hide :result :hide}}
(defn clerk-files
  []
  (try
    (some->> (str (fs/home) "/russmatney/clawe" "/src/notebooks")
             fs/list-dir (map str) (map ->f))
    (catch Exception _e
      (println "nothing at *file* " *file*)
      ;; (println e)
      nil)))

^{::clerk/visibility {:code :hide :result :hide}}
(def ^:dynamic *current-clerk-file*
  (atom (->f *file*)))


^{::clerk/visibility {:code :hide :result :hide}}
(defn rerender []
  (clerk/show! (:path @*current-clerk-file*)))

^{::clerk/visibility {:code :hide :result :hide}}
(defn set-file [f]
  (println "set file called" f)
  (reset! *current-clerk-file* f)
  (rerender))
