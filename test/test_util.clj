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

(def default-wait-death 5000)
(def default-wait-delay-ms 10)

(defn now [] (inst-ms (java.util.Date.)))

;; https://gist.github.com/kornysietsma/df45bbea3196adb5821b
(defn wait-until*
  "wait until a function has become true"
  ([name fn] (wait-until* name default-wait-death fn))
  ([name wait-death fn]
   (let [die (+ (now) wait-death)]
     (println "waiting for:" name)
     (loop []
       (if-let [result (fn)]
         result
         (do
           (Thread/sleep default-wait-delay-ms)
           (if (> (now) die)
             (throw (Exception. (str "timed out waiting for: " name)))
             (recur))))))))

(defmacro wait-until
  [msg ms expr]
  `(wait-until* ~msg ~ms (fn [] ~expr)))
