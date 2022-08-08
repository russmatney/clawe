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
  (->>
    conn
    ents-with-doctor-type
    (filter :org/tags)
    first
    ;; (dissoc :org/id)
    ;; (select-keys [:garden/file-name])
    )
  )

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

      (for [[i [k ents-for-type]]
            (->> ents
                 (group-by :doctor/type)
                 (map-indexed vector))]

        (let []
          (def ents-for-type ents-for-type)
          ^{:key i}
          [:div
           ;; doctor/type
           [:span
            {:class ["font-mono"]}
            (when (zero? i) (some-> k str))]

           [:span
            {:class ["font-nes" "pl-4"]}
            (count ents-for-type)

            (for [[i [tag group]]
                  (->>
                    ents-for-type
                    (group-by :org/tags)
                    (sort-by (comp count second))
                    reverse
                    (map-indexed vector))]
              (let []
                ^{:key i}
                [:div
                 [:span
                  {:class ["font-mono"]}
                  (or tag "(no tag)")]

                 [:span
                  {:class ["font-nes" "pl-4"]}
                  (count group)]]))]

           #_(for [[i ent] (->> ents-for-type
                                (sort-by :db/id)
                                (map-indexed vector))]
               (let []
                 (def ent ent)
                 ^{:key i}
                 [:div

                  (str "[:db/id " (:db/id ent) "]")
                  #_[components.garden/garden-node ent]

                  [components.garden/selected-node ent]

                  [components.debug/raw-metadata ent]]))]))]

     #_[:div
        {:class ["font-mono"]}
        (pr-str conn)]]))
