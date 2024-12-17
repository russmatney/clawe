(ns pages.db
  (:require
   [uix.core :as uix :refer [defui $]]

   [components.table :as components.table]
   [datascript.core :as d]
   [doctor.ui.views.ingest :as ingest]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [pages.db.tables :as tables]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db by :doctor/type
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ents-with-doctor-type [db]
  (when db
    (->> (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type _]]
              @db)
         (map first))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui page [opts]
  (let [ents       (:data
                    (hooks.use-db/use-query {:db->data ents-with-doctor-type}))
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
            {:class ["p-2"] :key i}
            ($ components.table/table table-def))))))
