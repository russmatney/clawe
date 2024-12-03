(ns doctor.ui.views.today
  (:require
   [hiccup-icons.octicons :as octicons]
   [tick.core :as t]
   [uix.core :as uix :refer [defui $]]

   [components.filter :as components.filter]
   [components.filter-defs :as filter-defs]
   [components.events :as components.events]
   [doctor.ui.db :as ui.db]
   ))

(def icon octicons/calendar16)


(defn presets []
  {:some-preset
   {:filters     #{}
    :group-by    :filters/event-timestamp
    :sort-groups :filters/event-timestamp
    :default     true}})


;; (defui widget [opts]
;;   (let [today  (t/today)
;;         events (ui.db/events
;;                  (:conn opts)
;;                  {:filter-by
;;                   #(some-> % :event/timestamp t/date (= today))})

;;         notes (ui.db/garden-notes
;;                 (:conn opts)
;;                 {:filter-by
;;                  #(some-> % :file/last-modified t/date (= today))})

;;         filter-data
;;         (components.filter/use-filter
;;           (assoc filter-defs/fg-config
;;                  :id (:filter-id opts :views-today)
;;                  :presets (presets)
;;                  :items (concat events notes)
;;                  :label (str (count events) " events today")))]
;;     ($ :div
;;        {:class ["p-4" "text-slate-200"]}
;;        ($ :div
;;           (:filter-grouper filter-data)

;;           ($ components.filter/items-by-group
;;              (assoc filter-data :group->comp components.events/event-cluster))))))
