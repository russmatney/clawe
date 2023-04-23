(ns doctor.ui.db
  (:require
   [datascript.core :as d]
   [dates.tick :as dt]
   [wing.core :as w]))

;; TODO tests for this namespace

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def event-types
  #{:type/commit
    :type/screenshot
    :type/lichess-game
    :type/note
    :type/todo})

(defn events
  ([conn] (events conn event-types))
  ([conn event-types]
   (when conn
     (->> (d/q '[:find (pull ?e [*])
                 :in $ ?event-types
                 :where
                 ;; TODO consider lower bound/min time here
                 [?e :event/timestamp ?ts]
                 [?e :doctor/type ?type]
                 [(contains? ?event-types ?type)]]
               conn event-types)
          (map first)
          (sort-by :event/timestamp dt/sort-latest-first)
          (take 200)))))

(defn chess-games [conn]
  (when conn
    (->> (d/q '[:find (pull ?e [*])
                :where
                ;; TODO consider lower bound/min time here
                [?e :doctor/type :type/lichess-game]]
              conn)
         (map first)
         (sort-by :event/timestamp dt/sort-latest-first)
         (take 200))))

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

(defn repos [conn]
  (when conn
    (->>
      (d/q '[:find (pull ?e [*])
             :where
             [?e :doctor/type :type/repo]]
           conn)
      (map first))))

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
         (sort-by :screenshot/time dt/sort-latest-first)
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
                [?e :doctor/type :type/note]
                [?e :org/level :level/root]]
              conn)
         (map first)
         (sort-by :file/last-modified dt/sort-latest-first)
         (take n))))))

(defn garden-files
  "Returns garden source-files"
  ([conn] (garden-files conn nil))
  ([conn {:keys [n]}]
   (when conn
     (let [n (or n 100)]
       (->>
         (d/q '[:find (pull ?e [:org/source-file :file/last-modified])
                :where
                [?e :doctor/type :type/note]
                [?e :org/source-file ?source-file]]
              conn)
         (map first)
         (w/distinct-by :org/source-file)
         (sort-by :file/last-modified dt/sort-latest-first)
         (map :org/source-file)
         (take n))))))

(defn join-children [conn items]
  (->> items
       (map (fn [td]
              (let [children
                    (->>
                      (d/q '[:find (pull ?c [*])
                             :in $ ?db-id
                             :where [?c :org/parents ?db-id]]
                           conn
                           (:db/id td))
                      (map first))]
                (assoc td :org/items children))))))

(defn list-todos-with-children
  ([conn] (list-todos-with-children conn nil))
  ([conn {:keys [n filter-pred] :as _opts}]
   (cond->>
       (d/q '[:find (pull ?e [*])
              :where [?e :doctor/type :type/todo]]
            conn)
     true        (map first)
     filter-pred (filter filter-pred)
     n           (take n)

     true (join-children conn))))

(defn list-todos
  ([conn] (list-todos conn nil))
  ([conn {:keys [n filter-pred join-children?]}]
   (when conn
     (let [n (or n 100)]
       (cond->>
           (d/q '[:find (pull ?e [*])
                  :where [?e :doctor/type :type/todo]]
                conn)
         true           (map first)
         filter-pred    (filter filter-pred)
         n              (take n)
         join-children? (join-children conn))))))

(defn current-todos [conn]
  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e :doctor/type :type/todo]
           (or
             [?e :org/status :status/in-progress]
             [?e :org/tags "current"])]
         conn)
    (map first)
    (join-children conn)))

;; TODO write fe unit tests for this and this whole ns
(defn garden-tags
  ([conn] (garden-tags conn nil))
  ([conn _opts]
   (->>
     (d/q '[:find ?tag
            :where [_ :org/tags ?tag]]
          conn)
     (map first)
     (into #{}))))
