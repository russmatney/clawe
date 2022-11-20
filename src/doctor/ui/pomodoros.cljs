(ns doctor.ui.pomodoros
  (:require
   [doctor.ui.localstorage :as localstorage]
   [clojure.edn :as edn]
   [tick.core :as t]))

(defn get-state []
  (or (some-> (localstorage/get-item "pomodoros")
              edn/read-string)
      {}))

(comment
  (localstorage/remove-item! "pomodoros")
  (edn/read-string (localstorage/get-item "pomodoros"))
  (->
    (edn/read-string (localstorage/get-item "pomodoros"))
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

(defn actions []
  (let [{:keys [current]} (get-state)]
    (->>
      [(when current
         {:on-click (fn [_] (end-current))
          :label    "End"})
       (when (not current)
         {:on-click (fn [_] (start-new))
          :label    "Start"})]
      (remove nil?))))
