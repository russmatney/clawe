(ns pages.journal
  (:require
   [components.garden :as components.garden]
   [doctor.ui.db :as ui.db]
   [plasma.uix :as plasma.uix :refer [with-rpc]]
   [doctor.ui.handlers :as handlers]))

(defn use-recent-garden-notes [conn]
  (let [notes             (plasma.uix/state nil)
        recent-file-paths (->> (ui.db/garden-files conn) (take 3))]
    (println "using recent garden notes" recent-file-paths)
    (with-rpc [conn]
      (when recent-file-paths
        (handlers/full-garden-items recent-file-paths))
      (fn [items]
        (println "rec items" (count items))
        (reset! notes items)))
    {:notes @notes}))


(defn page [{:keys [conn] :as opts}]
  (let [{:keys [notes]} (use-recent-garden-notes conn)]
    [:div
     "Journals"

     (for [[i note] (map-indexed vector notes)]
       [:div
        {:key   i
         :class [""]}

        [components.garden/org-file opts note]])]))
