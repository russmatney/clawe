(ns pages.journal
  (:require
   [uix.core :as uix :refer [$ defui]]
   [components.garden :as components.garden]
   [doctor.ui.db :as ui.db]
   ;; [plasma.uix :as plasma.uix :refer [with-rpc]]
   [doctor.ui.handlers :as handlers]))

(defn use-recent-garden-notes [conn]
  (let [notes             (uix/use-state nil)
        recent-file-paths (->> (ui.db/garden-files conn) (take 3))]
    ;; (with-rpc [conn]
    ;;   (when recent-file-paths
    ;;     (->> recent-file-paths
    ;;          (map (fn [p] {:org/source-file p}))
    ;;          handlers/full-garden-items))
    ;;   #(reset! notes %))
    {:notes notes}))

(defui page [{:keys [conn] :as opts}]
  (let [{:keys [notes]} (use-recent-garden-notes conn)]
    ($ :div
       (for [[i note] (map-indexed vector notes)]
         ($ :div
            {:key   i
             :class ["p-2"
                     "bg-slate-800"
                     "xl:mx-48" "mx-16" "mt-16"]}

            ($ components.garden/org-file opts note))))))
