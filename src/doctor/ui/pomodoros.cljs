(ns doctor.ui.pomodoros
  (:require
   [clojure.edn :as edn]
   [time-literals.read-write :as time-literals.read-write]
   [tick.core :as t]

   [doctor.ui.localstorage :as localstorage]))

(defn edn-read [x]
  (edn/read-string
    {:readers time-literals.read-write/tags}
    x))

(defn get-state []
  (or (some-> (localstorage/get-item "pomodoros")
              edn-read)
      {}))

(comment
  (localstorage/remove-item! "pomodoros")
  (edn-read (localstorage/get-item "pomodoros"))
  (->
    (edn-read (localstorage/get-item "pomodoros"))
    :obj :with-now)
  (localstorage/set-item! "pomodoros" (pr-str {:hi  :there
                                               :obj {:with-now (t/zoned-date-time)}})))

(defn start-new []
  (let [state (get-state)]
    (localstorage/set-item!
      "pomodoros"
      (pr-str
        (assoc state
               :current
               {:started-at (t/zoned-date-time)})))))

(defn end-current []
  (let [{:keys [current] :as state} (get-state)]
    (localstorage/set-item!
      "pomodoros"
      (pr-str
        (assoc state
               :current nil
               :last (assoc current :finished-at (t/zoned-date-time)))))))
