(ns item.core
  (:require
   [dates.tick :as dates.tick]))

(def time-keys
  #{:screenshot/time-string
    :clip/time-string
    :commit/author-date
    :org/closed
    :org/scheduled
    :org/deadline
    :org.prop/archive-time
    :org.prop/created-at
    :lichess.game/last-move-at
    :lichess.game/created-at})

(defn ->time-string [item]
  ((apply some-fn
          #(when (string? %) %) ;; pass strings through
          time-keys)
   item))

(comment
  (->time-string "x")
  (->time-string {:screenshot/time-string "x"})
  (->time-string {:commit/author-date "x"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item->event
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ts-keys
  {:type/commit       #{:commit/author-date}
   :type/screenshot   #{:screenshot/time}
   :type/clip         #{:clip/time}
   :type/lichess-game #{:lichess.game/created-at
                        :lichess.game/last-move-at}
   :type/todo         #{:org.prop/created-at
                        :org.prop/archive-time
                        :org/scheduled
                        :org/deadline
                        :org/closed
                        :file/last-modified}
   :type/note         #{:org.prop/created-at
                        :org.prop/archive-time
                        :org/scheduled
                        :org/deadline
                        :org/closed
                        :file/last-modified}})

(defn ->latest-timestamp
  "Returns the latest timestamp for the passed item."
  ([item] (->latest-timestamp nil item))
  ([type item]
   (let [type (or type (:doctor/type item))
         ks   (ts-keys type)]
     (some->> ks
              (map (fn [k]
                     (when-let [maybe-time (item k)]
                       (when-let [maybe-time
                                  (if (string? maybe-time)
                                    (dates.tick/parse-time-string maybe-time)
                                    maybe-time)]
                         (dates.tick/add-tz maybe-time)))))
              (remove nil?)
              (sort dates.tick/sort-latest-first)
              first))))
