(ns api.workspaces
  (:require
   [manifold.stream :as s]
   [taoensso.telemere :as log]
   [systemic.core :refer [defsys] :as sys]

   [clawe.wm :as wm]
   [util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Active workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-workspaces []
  (->>
    (wm/active-workspaces {:include-clients true})
    (map util/drop-complex-types)))

(comment
  (active-workspaces)
  (->> (active-workspaces) (filter :workspace/focused)))

(defsys ^:dynamic *workspaces-stream*
  :start (s/stream 1)
  :stop (s/close! *workspaces-stream*))

(def last-val (atom nil))

(comment
  (sys/start! `*workspaces-stream*)
  (sys/restart! `*workspaces-stream*))

(defn push-updated-workspaces []
  (let [wsp-data (active-workspaces)]
    ;; how expensive if this `=` check?
    (when-not (= @last-val wsp-data)
      (reset! last-val wsp-data)
      (let [res (s/try-put! *workspaces-stream* (active-workspaces) 0)]
        ;; (if @res
        ;;   (log/log! {:level :debug} "pushing updated workspaces")
        ;;   (log/log! {:level :debug} "dropping workspace update"))
        res))))

(comment
  (sys/start!)
  (push-updated-workspaces)

  (->>
    (repeatedly push-updated-workspaces)
    (take 5)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
