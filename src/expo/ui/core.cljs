(ns expo.ui.core
  (:require
   [uix.dom.alpha :as uix.dom]
   [uix.core.alpha :as uix]
   [wing.uix.router :as router]
   [taoensso.timbre :as log]
   [wing.core :as w]
   [datascript.core :as d]
   [pages.core :as pages]
   [expo.pages.home :as home]
   [promesa.core :as p]))

(def route-defs
  [{:route "/" :page-name :page/home :label "Home" :comp home/page}])

(def routes
  (->> route-defs
       (map (fn [{:keys [route page-name]}] [route {:name page-name}]))
       (into [])))

(defn db-hook []
  (let [db (uix/state nil)]
    (uix/with-effect [db]

      (-> (.fetch js/window "/expo-db.edn")
          (p/then #(.text %))
          (p/then (fn [raw-edn]
                    (println "conn-from-db" (d/conn-from-db raw-edn))
                    (reset! db (d/conn-from-db raw-edn))
                    ;; (reset! db "some-val")
                    (println "db reset" db)))))

    {:db @db}))

(defn view [opts]
  (let [{:keys [db]}       (db-hook)
        opts               (assoc opts :db db)
        page-name          (-> router/*match* uix/context :data :name)
        by-page-name       (w/index-by :page-name route-defs)
        {:keys [comp comp-only]
         :as   _route-def} (by-page-name page-name)]

    (if comp
      (if comp-only
        [comp opts]
        [pages/page route-defs comp opts])
      [:div "no page"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:dev/after-load mount-root []
  (uix.dom/render
    [wing.uix.router/router-provider
     {:routes routes}
     view
     {:x (js/Date.now)}]
    (.getElementById js/document "app")))

(defn dev-setup []
  (enable-console-print!))

(defn ^:export init
  []
  (log/info "Init Expo frontend")
  (dev-setup)
  (mount-root))
