(ns doctor.ui.actions
  (:require
   [doctor.ui.handlers :as handlers]))


(defn ->actions [item]
  (when (= (:doctor/type item) :type/wallpaper)
    (let [{:keys []} item]
      (->>
        [
         {:action/label    "Set as background"
          :action/on-click (fn [_]
                             (handlers/set-wallpaper item))}]
        (remove nil?)))))
