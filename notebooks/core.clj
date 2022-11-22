(ns notebooks.core
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]))

(defn ->f [path]
  (let [name (some-> path fs/file-name fs/split-ext first)]
    (if name
      {:path path
       :name name
       :uri  (-> (str "/notebooks/" name) (string/replace "_" "-"))}
      {:path path})))

(comment
  (->f *file*)
  (some-> *file* fs/file-name fs/split-ext first))

(defn project-root []
  ;; TODO unhardcode those - not sure why *file* doesn't work :/
  (str (fs/home) "/russmatney/clawe"))

(defn notebooks []
  (some->> (str (project-root) "/src/notebooks") fs/list-dir
           (remove fs/directory?)
           (remove (fn [path]
                     (or
                       (string/includes? path ".DS_Store")
                       (string/includes? path "core"))))
           (map str) (map ->f)))
