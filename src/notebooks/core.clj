^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.core
  (:require
   [nextjournal.clerk :as clerk]
   [notebooks.clerk :as notebooks.clerk]
   [notebooks.nav :as nav]
   [babashka.fs :as fs]))


^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer     nav/nav-viewer}
nav/nav-options

;; # Core

^{::clerk/visibility {:code :hide :result :hide}}
(defn rerender []
  (notebooks.clerk/update-open-notebooks))


(defn ->f [path]
  {:path path
   :name (some-> path
                 fs/file-name
                 fs/split-ext
                 first)})

(comment
  (->f *file*)
  (some-> *file* fs/file-name fs/split-ext first))

(defn notebooks
  []
  (try
    (some->>
      (str (fs/home) "/russmatney/clawe" "/src/notebooks")
      fs/list-dir (map str) (map ->f))
    (catch Exception _e
      (println "nothing at *file* " *file*)
      ;; (println e)
      nil)))
