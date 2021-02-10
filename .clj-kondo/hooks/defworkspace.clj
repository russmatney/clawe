(ns hooks.defworkspace
  (:require [clj-kondo.hooks-api :as api]))

(def required-keys [
                    ;; :workspace/title
                    ])

(defn defworkspace [{:keys [node]}]
  (let [name              (-> node api/sexpr second)
        x                 (-> node :children (nth 2 nil))
        _                 (prn "x" x)
        {:keys [row col]} (some-> x meta)
        ks                (-> node api/sexpr (nth 2 nil) keys set)]
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
                 :col     col})))))

    (prn "list" (list (-> node :children rest second)))
    (prn "node" (api/list-node (list (api/token-node 'def)
                                     (-> node :children second)
                                     (-> node :children rest second))))

    {:node (api/list-node (list (api/token-node 'def)
                                (-> node :children second)
                                (-> node :children rest second)
                                ))}))
