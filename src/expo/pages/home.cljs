(ns expo.pages.home
  (:require [datascript.core :as d]))

(defn page [opts]
  (let [db (:db opts)]
    [:div
     "Home"

     [:div
      (pr-str db)]]))
