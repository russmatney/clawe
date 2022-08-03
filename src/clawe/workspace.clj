(ns clawe.workspace
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [clawe.client :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema
  [:map
   [:workspace/title string?]
   [:workspace/directory string?]
   [:workspace/index int?]
   [:workspace/initial-file {:optional true} string?]
   ;; extra app names used to match on clients when applying clawe.rules
   [:workspace/app-names {:optional true} [:sequential string?]]
   [:workspace/clients {:optional true} [:sequential client/schema]]])


(comment
  (m/decode
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/app-names "hi"
     :gibber              :jabber}
    (mt/strip-extra-keys-transformer))

  (m/validate
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/index     0
     :workspace/app-names ["hi"]
     :gibber              :jabber}))

