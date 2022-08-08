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
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     [components.table/table
      {:headers [":doctor/type"
                 "Entities"]
       :rows    (concat
                  [["all" (count ents)]]
                  (map (fn [[type ents]]
                         [(str type) (count ents)])
                       ents-by-doctor-type))}]

     [:div
      {:class ["font-mono"]}

      (for [[i [doctor-type ents-for-type]]
            (->> ents-by-doctor-type (map-indexed vector))]
        ^{:key i}
        [components.table/table-for-doctor-type doctor-type ents-for-type])]]))
