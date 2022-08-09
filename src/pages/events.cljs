(ns pages.events
  (:require
   [datascript.core :as d]
   [components.events :as components.events]
   [tick.core :as t]
   [dates.tick :as dates.tick]))

(def event-types
  #{:type/commit
    :type/screenshot
    :type/lichess-game
    :type/garden})

(def ts-keys
  {:type/commit       #{:commit/author-date}
   :type/screenshot   #{:screenshot/time}
   :type/lichess-game #{:lichess.game/created-at
                        :lichess.game/last-move-at}
   :type/garden       #{:org.prop/created-at
                        :org.prop/archive-time
                        :org/scheduled
                        :org/deadline
                        :org/closed}})

(defn item->latest-timestamp
  "Returns the latest timestamp for the passed item."
  [{:keys [doctor/type] :as item}]
  (let [ks (ts-keys type)]
    (some->> ks
             (map (fn [k]
                    (when-let [maybe-time (item k)]
                      ;; HACK that jams a tz onto these... maybe it's fine?
                      (dates.tick/add-tz
                        (if (string? maybe-time)
                          (dates.tick/parse-time-string maybe-time)
                          maybe-time)))))
             (remove nil?)
             (sort-by t/>)
             first)))

(defn db-events [conn]
  (when conn
    (->> (d/q '[:find (pull ?e [*])
                :in $ ?event-types
                :where
                [?e :doctor/type ?type]
                [(?event-types ?type)]]
              conn event-types)
         (map first)
         (remove (comp nil? item->latest-timestamp))
         (sort-by item->latest-timestamp t/>)
         (take 200)
         (map (fn [ev]
                ;; HACK decorating on the frontend
                ;; we should be creating multiple events per item during ingestion
                (assoc ev :event/timestamp (item->latest-timestamp ev)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [{:keys [conn]}]
  (let [events (db-events conn)]
    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"]}

     [components.events/events-cluster nil events]]))
