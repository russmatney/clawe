(ns api.pomodoros
  (:require
   [taoensso.telemere :as log]
   [db.core :as db]
   [dates.tick :as dt]
   [ralphie.notify :as notify]))

(defn get-list []
  (->>
    (db/query '[:find (pull ?e [*])
                :where [?e :pomodoro/id _]])
    (map first)))

(defn get-state []
  (let [current (some->> (db/query '[:find (pull ?e [*])
                                     :where [?e :pomodoro/started-at _]])
                         (map first)
                         (sort-by :pomodoro/started-at dt/sort-latest-first)
                         first)
        last    (some->> (db/query '[:find (pull ?e [*])
                                     :where [?e :pomodoro/finished-at _]])
                         (map first)
                         (sort-by :pomodoro/finished-at dt/sort-latest-first)
                         first)]
    (if (dt/newer (:pomodoro/started-at current)
                  (:pomodoro/finished-at last))
      {:current current :last last}
      {:last last})))

(defn start-if-break []
  (let [current (:current (get-state))]
    (if current
      (log/log! :warn "Attempted to start-new pomodoro when current exists, doing nothing")
      (do
        (-> {:doctor/type         :type/pomodoro
             :pomodoro/started-at (dt/now)
             :pomodoro/is-current true
             :pomodoro/id         (random-uuid)}
            (db/transact))
        (notify/notify "Starting new pomodoro")))
    (get-state)))

;; TODO write some kind of 'smart-toggle' logic in here
;; e.g. if we're on a break, start the pomorodo
;; e.g. if we're beyond 2x our 'break' time, stop the pomodoro
;; e.g. if we're beyond 4/5x our 'break' time, start another one
(defn start-pomodoro
  "Starts a pomodoro regardless of the current state."
  []
  (-> {:doctor/type         :type/pomodoro
       :pomodoro/started-at (dt/now)
       ;; TODO drop this is-current usage
       :pomodoro/is-current true
       :pomodoro/id         (random-uuid)}
      (db/transact))
  (notify/notify "Starting new pomodoro")
  (get-state))

(defn end-current []
  (let [{:keys [current]} (get-state)]
    (when current
      (-> current
          (assoc :pomodoro/finished-at (dt/now))
          (assoc :pomodoro/is-current false)
          (db/transact))
      (notify/notify "Finished pomodoro"))
    (get-state)))

(comment
  (get-state)
  (start-if-break)
  (start-pomodoro)
  (end-current))



(defn clean-up-pomodoros []
  (->> (db/query '[:find (pull ?e [*])
                   :where [?e :pomodoro/id _]])
       (map first)
       (map :db/id)
       (db/retract-entities)))

(comment
  (clean-up-pomodoros))

(defn route
  "Routes an api request."
  [{:keys [uri _query-string] :as _req}]
  ;; (log/log! :debug "Routing API req" req (System/currentTimeMillis))

  (cond
    (= "/api/pomodoros" uri)
    (let [xs (get-list)]
      (println "listing pomodoros" xs)
      {:status 200 :body {:pomodoros xs}})

    :else
    {:status 404}))
