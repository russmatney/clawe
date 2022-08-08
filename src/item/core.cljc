(ns item.core)

(def time-keys
  #{:screenshot/time-string
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
