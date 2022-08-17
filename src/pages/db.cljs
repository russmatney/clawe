(ns pages.db
  (:require
   [datascript.core :as d]
   [components.table :as components.table]
   [pages.db.tables :as tables]
   [doctor.ui.handlers :as handlers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db by :doctor/type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ents-with-doctor-type [conn]
  (when conn
    (->> (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type _]]
              conn)
         (map first))))

(comment
  (declare conn)
  (->>
    conn
    ents-with-doctor-type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ingest buttons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ingest-buttons []
  [:div
   {:class ["grid" "grid-cols-6"
            "space-x-2"
            "p-2"]}
   (for [{:keys [label on-click]} (handlers/ingest-buttons)]
     ^{:key label}
     [:button {:class    ["bg-slate-800"
                          "p-4"
                          "border"
                          "border-slate-600"
                          "rounded-xl"
                          "font-mono"]
               :on-click on-click}
      label])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn] :as opts}]
  (let [ents       (ents-with-doctor-type conn)
        table-defs (tables/all-table-defs opts ents)]
    [:div
     {:class ["grid"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     [ingest-buttons]

     (for [[i table-def] (->> table-defs (map-indexed vector))]
       ^{:key i}
       [:div
        {:class ["p-2"]}
        [components.table/table table-def]])]))
