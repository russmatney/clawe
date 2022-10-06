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

(def this-file *file*)

(comment
  (->> *file* fs/parent fs/list-dir)
  )

(defn notebooks []
  (->> this-file fs/parent fs/list-dir
       (remove fs/directory?)
       (remove #(string/includes? % "core"))
       (map str) (map ->f)))
