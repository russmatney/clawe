(ns doctor.ui.actions
  (:require [hooks.db :as hooks.db]))


(defn ->actions [item]
  (when (= (:doctor/type item) :type/wallpaper)
    (let [{:keys []} item]
      (->>
        [
         {:action/label    "Set as background"
          :action/on-click (fn [_]
                             (hooks.db/set-wallpaper item))}]
        (remove nil?)))))
