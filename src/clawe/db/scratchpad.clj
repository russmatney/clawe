(ns clawe.db.scratchpad
  (:require [defthing.db :as db]))

(defn mark-buried
  "Adds the passed map to the db with a :scratchpads/buried-at timestamp."
  [id dat]
  (let [now (System/currentTimeMillis)]
    (db/transact [(-> dat
                      (assoc :scratchpad.db/id id)
                      (assoc :scratchpad.db/buried-at now))])))

(defn mark-restored
  "Adds the passed map to the db with a :scratchpads/restored-at timestamp."
  ([dat] (mark-restored (:scratchpad.db/id dat) dat))
  ([id dat]
   (let [now (System/currentTimeMillis)]
     (db/transact [(-> dat
                       (assoc :scratchpad.db/id id)
                       (assoc :scratchpad.db/restored-at now))]))))

(defn mark-all-restored
  "Marks all :scratchpad.db/id items restored, so no more will be found in
  future restore attempts (until something else is buried!)"
  []
  ;; TODO implement
  (db/transact []))

(defn next-restore
  "Returns the next scratchpad to restore."
  []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :scratchpad.db/buried-at ?buried-at]
        (or-join [?buried-at ?e]
                 ;; it was never restored
                 [(missing? $ ?e :scratchpad.db/restored-at)]
                 ;; or was buried more recently than restored
                 (and
                   [?e :scratchpad.db/restored-at ?restored-at]
                   [(> ?buried-at ?restored-at)]))])
    (map first)
    (sort-by :scratchpad.db/buried-at >)
    first))

(comment
  ;; TODO convert to unit test
  (def wsp1 {:some/name "wsp1" :my/custom "object"})
  (def wsp2 {:some/name "wsp2" :some.other/data "data"})

  (db/query
    '[:find (pull ?e [*])
      :where [?e :scratchpad.db/buried-at ?buried-at]])

  (mark-buried (:some/name wsp1) wsp1)
  (next-restore)

  (mark-buried (:some/name wsp2) wsp2)
  (db/dump)

  (mark-restored (:some/name wsp1) wsp1)
  (mark-restored (:some/name wsp2) wsp2)

  (next-restore)
  )
