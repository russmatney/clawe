(ns item.core)

(def time-keys
  #{:screenshot/time-string
    :git.commit/author-date
    :org/closed
    :org/scheduled
    :org/deadline
    :org.prop/archive-time
    :org.prop/created-at})

(defn ->time-string [item]
  ((apply some-fn
          #(when (string? %) %) ;; pass strings through
          time-keys)
   item))

(comment
  (->time-string "x")
  (->time-string {:screenshot/time-string "x"})
  (->time-string {:git.commit/author-date "x"}))
