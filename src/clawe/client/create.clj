(ns clawe.client.create
  (:require [clojure.string :as string]
            [babashka.process :as process]
            [clawe.config :as clawe.config]
            [ralphie.notify :as notify]
            [clawe.wm :as wm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exec [{:keys [exec/cmd]}]
  (-> cmd
      (string/split #" ")
      process/process
      process/check
      :out
      slurp))

(comment
  (exec {:exec/cmd "echo hi"}))

(defn- auto-resolve [f]
  (if-let [n (namespace f)]
    (do
      (require (symbol n))
      (ns-resolve (symbol n) f))
    (resolve f)))

(defn create-client
  {:org.babashka/cli
   {:alias {:key :client/key}}}
  [def]
  (let [def (cond
              (string? def)     (clawe.config/client-def def)
              (:client/key def) (clawe.config/client-def (:client/key def))
              :else             def)]
    (when-let [create-opts (-> def :client/create)]
      (cond
        ;; assume zero arity function
        (symbol? create-opts)
        (if-let [f (auto-resolve create-opts)]
          (f)
          (do
            (println "Could not resolve :client/create" create-opts)
            (notify/notify "Could not resolve :client/create")))

        ;; assume exec string
        (string? create-opts)
        (exec {:exec/cmd create-opts})

        ;; pull out function, pass rest to it as opts
        (map? create-opts)
        (if-let [cmd (-> create-opts :create/cmd)]
          (let [create-opts
                (->> create-opts
                     (map (fn [[k v]]
                            [k (cond
                                 ;; replace this template-val with the current workspace name
                                 (#{:create/use-workspace-title} v)
                                 (:workspace/title (wm/current-workspace))

                                 :else v)]))
                     (into {}))]
            ((auto-resolve cmd) create-opts))
          (notify/notify ":client/create map form requires :create/cmd"))))))

(comment
  (->
    (clawe.config/client-def "journal")
    create-client))
