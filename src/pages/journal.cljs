(ns pages.journal
  (:require
   [components.garden :as components.garden]
   [doctor.ui.db :as ui.db]
   [plasma.uix :as plasma.uix :refer [with-rpc]]
   [doctor.ui.handlers :as handlers]))

(defn use-recent-garden-notes [conn]
  (let [notes             (plasma.uix/state nil)
        recent-file-paths (->> (ui.db/garden-files conn) (take 3))]
    (with-rpc [conn]
      (when recent-file-paths
        (->> recent-file-paths
             (map (fn [p] {:org/source-file p}))
             handlers/full-garden-items))
      #(reset! notes %))
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
