(ns doctor.ui.db
  (:require
   [datascript.core :as d]
   [tick.core :as t]
   [dates.tick :as dates.tick]))

;; TODO tests for this namespace

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn events
  ([conn] (events conn event-types))
  ([conn event-types]
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
                 (assoc ev :event/timestamp (item->latest-timestamp ev))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commits-for-repo [conn repo]
  (when conn
    (d/q '[:find (pull ?e [*])
           :in $ ?dir
           :where
           [?e :doctor/type :type/commit]
           [?e :commit/directory ?dir]]
         conn
         (:repo/directory repo))))

(defn repo-for-commit [conn commit]
  (when conn
    (->>
      (d/q '[:find [(pull ?e [*])]
             :in $ ?dir
             :where
             [?e :doctor/type :type/repo]
             [?e :repo/directory ?dir]]
           conn
           (:commit/directory commit))
      first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn screenshots
  ([conn] (screenshots conn nil))
  ([conn {:keys [n]}]
   (when conn
     (let [n (or n 30)]
       (->>
         (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/screenshot]]
              conn)
         (map first)
         (sort-by :screenshot/time t/>)
         (take n))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpapers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wallpapers
  ([conn] (wallpapers conn nil))
  ([conn {:keys [n]}]
   (when conn
     (let [n (or n 30)]
       (->>
         (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/wallpaper]]
              conn)
         (map first)
         (sort-by :wallpaper/last-time-set >)
         (take n))))))
