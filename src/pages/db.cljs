(ns pages.db
  (:require
   [hooks.db :as hooks.db]
   [datascript.core :as d]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [{:keys [conn]} (hooks.db/use-db)
        data
        (when conn
          (println "conn" conn)
          (some->> (d/q '[:find (pull ?e [*])
                          :where ;; TODO create and use an expo type
                          [?e _ _]]
                        conn)
                   (map first)))]
    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"

              "text-white"]}

     "DB"

     [:div
      {:class ["font-mono"]}
      (pr-str conn)]

     [:div
      {:class ["font-mono"]}
      (doall
        (->> data
             (map str)))]

     ]))
