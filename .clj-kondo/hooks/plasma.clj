(ns hooks.plasma
  (:require [clj-kondo.hooks-api :as api]))

(defn defhandler
  "Defhandler clj-kondo hook"
  [ctx]
  (let [node                (:node ctx)
        [_ name & children] (:children node)]
    (if (= :clj (:lang ctx))
      {:node (api/list-node
               (list*
                 (api/token-node 'defn)
                 name
                 children))}
      {:node (api/token-node 'nil)})))
