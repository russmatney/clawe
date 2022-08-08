(ns pages.db
  (:require
   [hooks.db :as hooks.db]
   [datascript.core :as d]
   [components.debug :as components.debug]
   [components.garden :as components.garden]))

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
  (->
    conn
    ents-with-doctor-type
    first
    (dissoc :org/id)
    (select-keys [:garden/file-name])
    )
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [{:keys [conn]} (hooks.db/use-db)
        ents           (ents-with-doctor-type conn)]
    (def conn conn)
    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"

              "text-white"]}

     "DB"

     [:div
      {:class ["font-mono"]}

      (for [[i [k ents-by-type]]
            (->> ents
                 (group-by :doctor/type)
                 (map-indexed vector))]
        ^{:key i}
        [:div
         (when (zero? i)
           (some-> k str))

         (for [[i ent] (->> ents-by-type (map-indexed vector))]
           (let []
             (def ent ent)
             ^{:key i}
             [:div
              [components.garden/garden-node ent]

              #_[components.garden/org-file ent]

              [components.debug/raw-metadata ent]]))])]

     #_[:div
        {:class ["font-mono"]}
        (pr-str conn)]]))
