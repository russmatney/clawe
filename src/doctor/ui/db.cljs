(ns doctor.ui.db
  (:require
   [datascript.core :as d]
   [tick.core :as t]
   [dates.tick :as dates.tick]
   [item.core :as item]))

;; TODO tests for this namespace

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def event-types
  #{:type/commit
    :type/screenshot
    :type/lichess-game
    :type/garden})

(defn events
  ([conn] (events conn event-types))
  ([conn event-types]
   (when conn
     (->> (d/q '[:find (pull ?e [*])
                 :in $ ?event-types
                 :where
                 [?e :doctor/type ?type]
                 [?e :event/timestamp ?ts]
                 ;; TODO consider lower bound/min time here
                 ]
               conn event-types)
          (map first)
          (sort-by :event/timestamp t/>)
          (take 200)))))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden-notes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn garden-notes
  ([conn] (garden-notes conn nil))
  ([conn {:keys [n]}]
   (when conn
     (let [n (or n 100)]
       (->>
         (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/garden]]
              conn)
         (map first)
         (sort-by :org/created-at >)
         (take n))))))
