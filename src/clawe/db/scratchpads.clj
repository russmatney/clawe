(ns clawe.db.scratchpads
  (:require [clawe.db.core :as db]))


(defn mark-buried
  "Adds the passed map to the db with a :scratchpads/buried-at timestamp.

  Supports burying and restoring scratchpad positions."
  [id dat]
  (let [now (System/currentTimeMillis)]
    (db/transact [(-> dat
                      (assoc :scratchpad.db/id id)
                      (assoc :scratchpad.db/buried-at now))])))

(defn mark-restored
  "Adds the passed map to the db with a :scratchpads/restored-at timestamp.

  Supports burying and restoring scratchpad positions."
  [id dat]
  (let [now (System/currentTimeMillis)]
    (db/transact [(-> dat
                      (assoc :scratchpad.db/id id)
                      (assoc :scratchpad.db/restored-at now))])))

(defn clear-buried
  "Sets :scratchpads/bury-cleared true on all :scratchpads/buried-at,
  signaling that none of the stored scratchpads should ever be restored.

  Supports burying and restoring scratchpad positions."
  [dat]
  (db/transact [(assoc dat :scratchpad.db/bury-cleared true)]))

(defn last-buried []
  (db/query
   '[:find (pull ?e [*])
     :where
     ;; TODO refactor to sort/get actual latest buried!
     [?e :scratchpad.db/buried-at ?buried-at]
     ;; [(and
     ;;    [(missing? $ ?e :scratchpad.db/restored-at)]
     ;;    [(and
     ;;       [?e :scratchpad.db/restored-at ?restored-at]
     ;;       [(> ?buried-at ?restored-at)])])]
     (and
      [?e :scratchpad.db/bury-cleared ?bury-cleared]
      [(not= ?bury-cleared true)])]))

(comment
  (def wsp1 {:some/name "wsp1" :my/custom "object"})
  (def wsp2 {:some/name "wsp2" :some.other/data "data"})

  (mark-buried (:some/name wsp1) wsp1)
  (mark-buried (:some/name wsp2) wsp2)
  (db/dump)

  (mark-restored (:some/name wsp1) wsp1)

  (last-buried)
  )
