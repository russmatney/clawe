(ns doctor.ui.db
  (:require
   [taoensso.timbre :as log]
   [datascript.core :as d]
   [dates.tick :as dt]
   [wing.core :as w]))

(defn take-and-log [{:keys [n label]} xs]
  (if-not n
    xs
    (let [ct (count xs)]
      (when (> ct n) (log/info ct label "in db, trimming to" n))
      (->> xs (take n)))))

;; TODO tests for this namespace

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-event-types
  #{:type/commit
    :type/screenshot
    :type/clip
    :type/lichess-game
    :type/note
    :type/todo})

(defn events
  ([conn] (events conn nil))
  ([conn {:keys [event-types filter-by]}]
   (when conn
     (let [event-types (or event-types default-event-types)
           n           200]
       (cond->> (d/q '[:find (pull ?e [*])
                       :in $ ?event-types
                       :where
                       ;; TODO consider lower bound/min time here
                       [?e :event/timestamp ?ts]
                       [?e :doctor/type ?type]
                       [(contains? ?event-types ?type)]]
                     @conn event-types)
         true      (map first)
         filter-by (filter filter-by)
         true      (sort-by :event/timestamp dt/sort-latest-first)
         n         (take-and-log {:n n :label "events"}))))))

(defn chess-games [conn]
  (when conn
    (let [n 200]
      (->> (d/q '[:find (pull ?e [*])
                  :where
                  ;; TODO consider lower bound/min time here
                  [?e :doctor/type :type/lichess-game]]
                @conn)
           (map first)
           (sort-by :event/timestamp dt/sort-latest-first)
           (take-and-log {:n n :label "chess games"})))))

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
         @conn
         (:repo/directory repo))))

(defn repo-for-commit [conn commit]
  (when conn
    (->>
      (d/q '[:find [(pull ?e [*])]
             :in $ ?dir
             :where
             [?e :doctor/type :type/repo]
             [?e :repo/directory ?dir]]
           @conn
           (:commit/directory commit))
      first)))

(defn repos [conn]
  (when conn
    (->>
      (d/q '[:find (pull ?e [*])
             :where
             [?e :doctor/type :type/repo]]
           @conn)
      (map first))))

(def watched-usernames
  #{"russmatney"}
  )

(defn watched-repos [conn]
  (when conn
    (->>
      (d/q '[:find (pull ?e [*])
             :in $ ?watched-usernames
             :where
             [?e :doctor/type :type/repo]
             [?e :repo/user-name ?username]
             [(contains? ?watched-usernames ?username)]]
           @conn
           watched-usernames)
      (map first))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; pomodoros
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pomodoro-state [conn]
  (let [current (some->>
                  (d/q '[:find (pull ?e [*])
                         :where [?e :pomodoro/started-at _]] @conn)
                  (map first)
                  (sort-by :pomodoro/started-at dt/sort-latest-first)
                  first)
        last    (some->>
                  (d/q '[:find (pull ?e [*])
                         :where [?e :pomodoro/finished-at _]] @conn)
                  (map first)
                  (sort-by :pomodoro/finished-at dt/sort-latest-first)
                  first)]
    (if (dt/newer (:pomodoro/started-at current)
                  (:pomodoro/finished-at last))
      {:current current :last last}
      {:last last})))

(defn pomodoros [conn]
  (when conn
    (->>
      (d/q '[:find (pull ?e [*])
             :where
             [?e :doctor/type :type/pomodoro]]
           @conn)
      (map first)
      (filter :pomodoro/started-at)
      (sort-by :pomodoro/started-at dt/sort-latest-first))))

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
              @conn)
         (map first)
         (sort-by :wallpaper/last-time-set >)
         (take-and-log {:n n :label "wallpapers"}))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden-notes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn join-children [conn items]
  (->> items
       (map (fn [item]
              (let [children
                    (->>
                      (d/q '[:find (pull ?c [*])
                             :in $ ?db-id
                             :where [?c :org/parents ?db-id]]
                           @conn
                           (:db/id item))
                      (map first))]
                (assoc item :org/items children))))))

(defn garden-notes
  ([conn] (garden-notes conn nil))
  ([conn {:keys [n]}]
   (when conn
     (->>
       (d/q '[:find (pull ?e [*])
              :where
              [?e :doctor/type :type/note]]
            @conn)
       (map first)
       (sort-by :file/last-modified dt/sort-latest-first)
       (take-and-log {:n n :label "garden notes"})))))

(defn root-notes
  ([conn] (garden-notes conn nil))
  ([conn {:keys [n join-children?]}]
   (when conn
     (cond->>
         (d/q '[:find (pull ?e [*])
                :where
                [?e :doctor/type :type/note]
                [?e :org/level :level/root]]
              @conn)
       true           (map first)
       true           (sort-by :file/last-modified dt/sort-latest-first)
       n              (take-and-log {:n n :label "garden notes"})
       join-children? (join-children conn)))))

(defn garden-files
  "Returns garden source-files"
  ([conn] (garden-files conn nil))
  ([conn {:keys [n]}]
   (when conn
     (->>
       (d/q '[:find (pull ?e [:org/source-file :file/last-modified])
              :where
              [?e :doctor/type :type/note]
              [?e :org/source-file ?source-file]]
            @conn)
       (map first)
       (w/distinct-by :org/source-file)
       (sort-by :file/last-modified dt/sort-latest-first)
       (map :org/source-file)
       (take-and-log {:n n :label "garden files"})))))

(defn list-todos
  "Returns all todos in the db.

  when `join-children?` is true, subtasks are filtered from the returned
  list, but included on parent tasks as children.
  "
  ([conn] (list-todos conn nil))
  ([conn {:keys [n filter-pred join-children? skip-subtasks?]}]
   (when conn
     (let [n (or n 1000)]
       (cond->>
           (d/q
             (if skip-subtasks?
               '[:find (pull ?e [*])
                 :where [?e :doctor/type :type/todo]
                 ;; skip sub-tasks
                 (not [?e :org/parents ?p]
                      [?p :org/status _])]
               '[:find (pull ?e [*])
                 :where [?e :doctor/type :type/todo]])
             @conn)
         true           (map first)
         filter-pred    (filter filter-pred)
         n              (take-and-log {:n n :label "todos"})
         join-children? (join-children conn))))))

(defn current-todos [conn]
  (->>
    (d/q '[:find (pull ?e [*])
           :where
           [?e :doctor/type :type/todo]
           (or
             [?e :org/status :status/in-progress]
             ;; 'current' DEPRECATED delete soon
             ;; [?e :org/tags "current"]
             )
           ;; filter out todos with lingering 'current' tags but completed statuses
           (not
             [?e :org/status ?status]
             [(contains? #{:status/done
                           :status/skipped
                           :status/cancelled} ?status)])]
         @conn)
    (map first)
    (join-children conn)))

;; TODO write fe unit tests for this and this whole ns
(defn garden-tags
  ([conn] (garden-tags conn nil))
  ([conn _opts]
   (->>
     (d/q '[:find ?tag
            :where [_ :org/tags ?tag]]
          @conn)
     (map first)
     (into #{}))))
