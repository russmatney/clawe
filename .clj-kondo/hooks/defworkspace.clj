(ns hooks.defworkspace
  (:require [clj-kondo.hooks-api :as api])
  )

(def required-keys [:workspace/title])

(defn defworkspace [{:keys [node]}]
  (let [name              (-> node api/sexpr second)
        {:keys [row col]} (-> node :children last meta)
        ks                (-> node api/sexpr last keys set)]
    (->> required-keys
         (remove ks)
         ((fn [missing-keys]
            (when (seq missing-keys)
              (api/reg-finding!
                {:message (str "'" name
                               "' defworkspace missing required key"
                               (if (> (count missing-keys) 1)
                                 "s: "
                                 ": ")
                               (apply str missing-keys))
                 :type    :clawe/missing
                 :row     row
                 :col     col})))))))
