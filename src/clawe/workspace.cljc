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
   [:workspace/title :string]
   [:workspace/directory {:optional true} :string]
   [:workspace/focused {:optional true} [:maybe :boolean]]
   [:workspace/index {:optional true} int?]
   ;; extra app names used to match on clients when applying clawe.rules
   ;; TODO not workspace app-names, probably more like workspace/client-keys
   [:workspace/app-names {:optional true} [:sequential string?]]
   [:workspace/clients {:optional true} [:sequential client/schema]]])

(defn strip [w]
  (m/decode schema w (mt/strip-extra-keys-transformer)) )

(comment
  (strip
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/app-names "hi"
     :workspace/clients   [{:client/window-title "blah"
                            :some/other          "data"}]
     :gibber              :jabber})

  (m/decode
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/app-names "hi"
     :workspace/clients   [{:client/window-title "blah"
                            :some/other          "data"}]
     :gibber              :jabber}
    (mt/strip-extra-keys-transformer))

  (m/validate
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/index     0
     :workspace/app-names ["hi"]
     :gibber              :jabber}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; match
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn match?
  "Returns true if the passed workspaces have the same title and index."
  [a b]
  (and
    (= (:workspace/title a) (:workspace/title b))
    (= (:workspace/index a) (:workspace/index b))))

(defn find-matching-clients
  "Expects the passed workspace to have :workspace/clients included"
  [wsp client]
  (->> wsp :workspace/clients (filter (partial client/match? client))))

(defn find-matching-client
  "Expects the passed workspace to have :workspace/clients included"
  [wsp client]
  (let [matches (find-matching-clients wsp client)]
    (if (> (count matches) 1)
      (do
        (println "WARN: multiple matching clients found in workspace" (strip wsp) (strip client))
        (first matches))
      (some->> matches first))))
