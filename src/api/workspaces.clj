(ns api.workspaces
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [malli.transform :as mt]
   [malli.core :as m]
   [clawe.workspaces :as clawe.workspaces]
   [util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Active workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-workspaces []
  (->>
    (clawe.workspaces/all-workspaces)
    (filter :awesome.tag/name)
    (map clawe.workspaces/apply-git-status)
    (map util/drop-complex-types)))

(defsys *workspaces-stream*
  :start (s/stream)
  :stop (s/close! *workspaces-stream*))

(comment
  (sys/start! `*workspaces-stream*))

(defn update-workspaces []
  (println "pushing to workspaces stream (updating topbar)!")
  (s/put! *workspaces-stream* (active-workspaces)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fe-workspace
  "A watered-down workspace, without functions and lists"
  [:map
   ;; [:rules/is-my-client? fn?]
   ;; [:awesome/rules ]
   [:workspace/readme string?]
   [:defthing.core/registry-key keyword?]
   [:ns string?]
   [:name string?]
   [:type keyword?]
   [:git/repo string?]
   [:awesome.tag/selected boolean?]
   [:workspace/initial-file string?]
   [:workspace/updated-at int?] ;; TODO time
   [:workspace/scratchpad boolean?]
   [:workspace/directory string?]
   [:workspace/exec string?] ;; TODO or tmux/fire map?
   [:awesome.tag/index int?]
   [:db/id int?]
   [:workspace/scratchpad-class string?]
   [:awesome.tag/layout string?]
   ;; [:awesome.tag/clients ]
   [:awesome.tag/name string?]
   [:workspace/title string?]
   ;; [:awesome/tag ]
   [:awesome.tag/empty boolean?]
   [:awesome.tag/urgent boolean?]])


(comment
  (def -w
    (->> (clawe.workspaces/all-workspaces)
         (filter :awesome.tag/name)
         first
         ))

  (m/decode
    fe-workspace
    -w
    (mt/strip-extra-keys-transformer))


  (->>
    (active-workspaces)
    (sort-by :awesome.tag/index)
    first)

  (update-workspaces)

  )
