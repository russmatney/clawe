(ns doctor.build-hooks
  (:require [ralphie.notify :as notify]))

(defn notify-complete
  {:shadow.build/stage :flush}
  [build-state]
  (notify/notify "Build Complete" (str (:shadow.build/build-id build-state)))
  build-state)
