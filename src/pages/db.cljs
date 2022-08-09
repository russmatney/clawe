(ns pages.db
  (:require
   [hooks.db :as hooks.db]
   [datascript.core :as d]
   [components.table :as components.table]
   [pages.db.tables :as tables]))

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
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn] :as opts}]
  (let [ents                (ents-with-doctor-type conn)
        ents-by-doctor-type (->> ents (group-by :doctor/type))]
    (def conn conn)
    [:div
     {:class ["grid"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     [:div
      {:class ["grid" "grid-cols-6"
               "space-x-2"
               "p-2"]}
      (for [{:keys [label on-click]}
            [{:label    "Ingest clawe repos"
              :on-click (fn [_] (hooks.db/ingest-clawe-repos))}
             {:label    "Ingest lichess games"
              :on-click (fn [_] (hooks.db/ingest-lichess-games))}
             {:label    "Clear lichess cache"
              :on-click (fn [_] (hooks.db/clear-lichess-games-cache))}
             {:label    "Ingest screenshots"
              :on-click (fn [_] (hooks.db/ingest-screenshots))}
             {:label    "Ingest wallpapers"
              :on-click (fn [_] (hooks.db/ingest-wallpapers))}]]
        [:button {:class    ["bg-slate-800"
                             "p-4"
                             "border"
                             "border-slate-600"
                             "rounded-xl"
                             "font-mono"]
                  :on-click on-click}
         label])]

     [:div
      {:class ["p-2"]}
      [components.table/table
       {:headers [":doctor/type"
                  "Entities"]
        :rows    (concat
                   [["all" (count ents)]]
                   (map (fn [[type ents]]
                          [(str type) (count ents)])
                        ents-by-doctor-type))}]]

     [:div
      [:div
       {:class ["p-2"]}
       [components.table/table
        (tables/garden-by-tag-table-def (:type/garden ents-by-doctor-type))]]

      (for [[i [doctor-type ents-for-type]]
            (->> ents-by-doctor-type (map-indexed vector))]
        ^{:key i}
        [:div
         {:class ["p-2"]}
         [tables/table-for-doctor-type opts doctor-type ents-for-type]])]]))
