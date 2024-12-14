(ns pages.db
  (:require
   [uix.core :as uix :refer [defui $]]

   [datascript.core :as d]
   [components.table :as components.table]
   [pages.db.tables :as tables]
   [doctor.ui.views.ingest :as ingest]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db by :doctor/type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ents-with-doctor-type [conn]
  (when conn
    (->> (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type _]]
              @conn)
         (map first))))

(comment
  (declare conn)
  (->>
    conn
    ents-with-doctor-type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui page [{:keys [conn] :as opts}]
  (let [ents       (ents-with-doctor-type conn)
        table-defs (tables/all-table-defs opts ents)]
    ($ :div
       {:class ["grid"
                "min-h-screen"
                "overflow-hidden"
                "bg-yo-blue-700"]}

       ($ ingest/ingest-buttons)
       ($ ingest/commit-ingest-buttons opts)

       (for [[i table-def] (->> table-defs (map-indexed vector))]
         ($ :div
            {:class ["p-2"] :key i }
            ($ components.table/table table-def))))))
