(ns test-util
  (:require
   [clojure.test :as t]
   [malli.core :as m]
   [malli.error :as me]))


;; https://github.com/metosin/malli/issues/369#issuecomment-797730510
(defmethod t/assert-expr 'valid
  [msg [_ schema data]]
  `(let [is-valid?# (m/validate ~schema ~data)]
     (t/do-report {:actual   ~data
                   :expected (-> ~schema
                                 (m/explain ~data)
                                 (me/humanize))
                   :message  ~msg
                   :type     (if is-valid?# :pass :fail)})))
