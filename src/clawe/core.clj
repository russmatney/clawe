(ns clawe.core
  (:require
   [ralph.defcom :as defcom :refer [defcom]]))

(defcom hello
  {:name    "hello"
   :handler (fn [_config _parsed]
              (println "Howdy!"))})

(defn -main [& args]
  (apply defcom/run args))
