(ns ralphie.util
  (:require
   [babashka.process :refer [$ check]]))

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  (-> ^{:dir dir}
      ($ clojure -Spath)
      check :out slurp))
