(ns ralphie.hyprland
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]
   ))

(defn hc! [msg]
  "fires hyprctl, returns json"
  (let [cmd (str "hyprctl -j " msg)]
    (-> (process/process
          {:cmd cmd :out :string})
        ;; throws when error occurs
        (process/check)
        :out
        (json/parse-string
          (fn [k] (keyword "hypr" k))))))

(comment
  (hc! "workspaces")
  (hc! "--help")
  )

(defn monitors []
  (hc! "monitors all"))

(defn workspaces []
  (hc! "workspaces"))

(defn layers []
  (hc! "layers"))
