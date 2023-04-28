(ns api.pomodoros
  (:require
   [taoensso.timbre :as log]
   [db.core :as db]
   [dates.tick :as dt]))


(defn get-state []
  (let [current (some->> (db/query '[:find (pull ?e [*])
                                     :where [?e :pomodoro/is-current true]])
                         ffirst)
        last    (some->> (db/query '[:find (pull ?e [*])
                                     :where [?e :pomodoro/finished-at _]])
                         (map first)
                         (sort-by :pomodoro/finished-at dt/sort-latest-first)
                         first)]
    {:current current :last last}))

(defn start-new []
  (let [current (:current (get-state))]
    (if current
      (log/warn "Attempted to start-new pomodoro when current exists, doing nothing")
      (-> {:doctor/type         :type/pomodoro
           :pomodoro/started-at (dt/now)
           :pomodoro/is-current true
           :pomodoro/id         (random-uuid)}
          (db/transact)))
    (get-state)))

(defn end-current []
  (let [{:keys [current]} (get-state)]
    (-> current
        (assoc :pomodoro/finished-at (dt/now))
        (assoc :pomodoro/is-current false)
        (db/transact))
    (get-state)))

(comment
  (get-state)
  (start-new)
  (end-current))


(defn clean-up-pomodoros []
  (->> (db/query '[:find (pull ?e [*])
                   :where [?e :pomodoro/id _]])
       (map first)
       (map :db/id)
       (db/retract-entities)))

(comment
  (clean-up-pomodoros))
