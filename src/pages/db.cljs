(ns pages.db
  (:require
   [hooks.db :as hooks.db]
   [datascript.core :as d]
   [components.table :as components.table]
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
  (->>
    conn
    ents-with-doctor-type))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn garden-note [ent]
  [:div
   (str "[:db/id " (:db/id ent) "]")
   #_[components.garden/garden-node ent]

   [components.garden/selected-node ent]

   [components.debug/raw-metadata ent]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [{:keys [conn]}      (hooks.db/use-db)
        ents                (ents-with-doctor-type conn)
        ents-by-doctor-type (->> ents (group-by :doctor/type))]
    (def conn conn)
    [:div
     {:class ["grid"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     [:div {:class ["grid" "grid-cols-6"]}
      (for [{:keys [label on-click]}
            [
             {:label    "Ingest lichess games"
              :on-click (fn [_] (hooks.db/ingest-lichess-games))}
             {:label    "Ingest screenshots"
              :on-click (fn [_] (hooks.db/ingest-screenshots))}]
            ]
        [:button {:class    ["bg-slate-800"
                             "p-4"
                             "rounded-xl"]
                  :on-click on-click}
         label])
      ]

     [components.table/table
      {:headers [":doctor/type"
                 "Entities"]
       :rows    (concat
                  [["all" (count ents)]]
                  (map (fn [[type ents]]
                         [(str type) (count ents)])
                       ents-by-doctor-type))}]

     [:div

      [components.table/table
       (components.table/garden-by-tag-table-def (:type/garden ents-by-doctor-type))]

      (for [[i [doctor-type ents-for-type]]
            (->> ents-by-doctor-type (map-indexed vector))]
        ^{:key i}
        [components.table/table-for-doctor-type doctor-type ents-for-type])]]))
