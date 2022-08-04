(ns test-util
  (:require
   [clojure.test :as t]
   [malli.core :as m]
   [malli.error :as me]
   [lambdaisland.specmonstah.malli :as sm-malli]
   [reifyhealth.specmonstah.core :as sm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; specmonstah
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gen-data
  "Expects a specmonstah `schema` and `query`"
  [schema query]
  (let [ent-db    (sm-malli/ent-db-spec-gen {:schema schema} query)
        by-type   (-> ent-db sm/ents-by-type)
        with-data (-> ent-db (sm/attr-map :spec-gen))]
    (->> by-type
         (map (fn [[type ents]]
                [type (->> ents
                           (map (fn [ent-name]
                                  [ent-name (get with-data ent-name)]))
                           (into {}))]))
         (into {}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wait until
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def default-wait-death 5000)
(def default-wait-delay-ms 10)

(defn now [] (inst-ms (java.util.Date.)))

;; https://gist.github.com/kornysietsma/df45bbea3196adb5821b
(defn wait-until*
  "wait until a function has become true"
  ([name fn] (wait-until* name default-wait-death fn))
  ([name wait-death fn]
   (let [die (+ (now) wait-death)]
     (loop []
       (if-let [result (fn)]
         result
         (do
           (println "waiting for [" name "] to complete")
           (Thread/sleep default-wait-delay-ms)
           (if (> (now) die)
             (throw (Exception. (str "timed out waiting for: " name)))
             (recur))))))))

(defmacro wait-until
  [msg ms expr]
  `(wait-until* ~msg ~ms (fn [] ~expr)))
