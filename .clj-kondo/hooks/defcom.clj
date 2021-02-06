(ns hooks.defcom
  (:require [clj-kondo.hooks-api :as api]))

(def required-keys [:defcom/handler
                    :defcom/name])

(defn defcom [{:keys [node]}]
  (let [name              (-> node api/sexpr second)
        {:keys [row col]} (-> node :children last meta)
        ks                (-> node api/sexpr last keys set)]
    (->> required-keys
         (remove ks)
         ((fn [missing-keys]
            (when (seq missing-keys)
              (api/reg-finding!
                {:message (str "'" name
                               "' defcom missing required key"
                               (if (> (count missing-keys) 1)
                                 "s: "
                                 ": ")
                               (apply str missing-keys))
                 :type    :defcom/missing
                 :row     row
                 :col     col})))))

    {:node (api/list-node (list* (api/token-node 'def)
                                 (-> node :children second)
                                 (-> node :children rest rest)))}))
