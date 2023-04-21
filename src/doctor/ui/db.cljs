(ns doctor.ui.db
  (:require
   [datascript.core :as d]
   [tick.core :as t]
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
                [?e :doctor/type :type/note]]
              conn)
         (map first)
         (sort-by :org/created-at >)
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
         (sort-by :file/last-modified >)
         (map :org/source-file)
         (take n))))))

(defn garden-todos
  ([conn] (garden-todos conn nil))
  ([conn {:keys [n filter-pred]}]
   (when conn
     (let [n (or n 100)]
       (->>
         (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/todo]
                [?e :org/status ?status]]
              conn)
         (map first)
         (sort-by :org/created-at >)
         ((fn [todos] (if filter-pred (->> todos (filter filter-pred)) todos)))
         (take n))))))

(defn queued-todos [conn]
  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e :todo/queued-at ?queued-at]]
         conn)
    (map first)
    (remove (comp #{:status/cancelled :status/done} :org/status))
    (sort-by :todo/queued-at >)))

(defn garden-current-items [conn]
  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e :doctor/type ?type]
           [(contains? #{:type/note :type/todo} ?type)]
           ;; TODO debug this, seems like it should work
           [?e :org/tags "current"]
           #_[(contains? ?tags "current")]]
         conn)
    (map first)
    (remove (comp #{:status/cancelled :status/done} :org/status))
    (sort-by :todo/queued-at >)))

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
