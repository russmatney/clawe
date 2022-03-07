(ns clawe.scratchpad
  (:require
   [ralphie.awesome :as awm]
   [clawe.awesome :as c.awm] ;; DEPRECATED
   [clawe.workspaces.create :as wsp.create]
   [clawe.db.scratchpad :as db.scratchpad]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focus-scratchpad
  "Passes opts to focus-client ensuring that all other windows are buried/tiled,
  the passed client is floated, put ontop, and centered."
  [client]
  ;; DEPRECATED
  (c.awm/focus-client
    {:bury-all? true
     :float?    true
     :center?   true}
    client))

(defn toggle-scratchpad
  "Expects the passed wsp to have :awesome/tag and :awesome/clients metadata.

  ;; TODO refactor workspace assumptions out
  ;; maybe just expect tag/client/create-hook
  "
  [wsp]
  (when wsp
    (let [wsp-name              (some wsp [:workspace/title])
          tag?                  (-> wsp :awesome.tag/name seq)
          clients               (-> wsp :awesome.tag/clients seq)
          client-classes        (-> wsp
                                    :workspace/scratchpad-classes
                                    (conj (:workspace/scratchpad-class wsp))
                                    (->>
                                      (remove nil?)
                                      (into #{})))
          client                (if (seq client-classes)
                                  (some->> clients (filter (comp client-classes :awesome.client/class)) first)
                                  (some->> clients first))
          rules-fn              (-> wsp :rules/apply)
          client-in-another-wsp (when (and (not client) (seq client-classes))
                                  ;; TODO refactor in a workspace client-predicate pattern
                                  ;; (rather than matching on a class like this)
                                  ;; could even just have the workspace-pred-fn return the client directly
                                  (->> (awm/all-clients)
                                       (filter (comp client-classes :awesome.client/class))
                                       first))
          centerwork?           (= (:awesome.tag/layout wsp) "centerwork")
          is-master?            (:awesome.client/master client)]
      (cond
        ;; "found selected tag, client for:" wsp-name
        (and tag? client (:awesome.tag/selected wsp))
        (if (or (:awesome.client/ontop client) (and centerwork? is-master?))
          (do
            ;; hide this tag
            (awm/toggle-tag wsp-name)

            ;; restore last buried client
            (let [to-restore (db.scratchpad/next-restore)]
              (when (and to-restore
                         ;; TODO shouldn't we already be able to answer this via fetch-tags ?
                         (awm/client-on-tag?
                           to-restore
                           (awm/awm-fnl
                             {:quiet? true}
                             '(-> (awful.screen.focused)
                                  (. :selected_tags)
                                  (lume.map (fn [t] {:name t.name}))
                                  (lume.first)
                                  (. :name)))))
                ;; DEPRECATED
                (c.awm/focus-client
                  {:bury-all? true
                   :float?    true
                   :center?   false}
                  client)

                (db.scratchpad/mark-restored to-restore))))
          ;; DEPRECATED
          (c.awm/focus-client
            {:bury-all? true
             :float?    true
             :center?   false}
            client))

        ;; "found unselected tag, client for:" wsp-name
        (and tag? client (not (:awesome.tag/selected wsp)))
        (do
          (awm/toggle-tag wsp-name)

          ;; DEPRECATED
          (c.awm/focus-client
            {:bury-all? true
             :float?    true
             :center?   false}
            client))

        ;; apply rules if another client was found
        (and rules-fn client-in-another-wsp)
        (rules-fn)

        ;; tag exists, no client
        (and tag? (not client))
        ;; create the client
        (wsp.create/create-client wsp)

        ;; tag does not exist, presumably no client either
        (not tag?)
        (do
          (awm/create-tag! wsp-name)
          (awm/toggle-tag wsp-name)
          (wsp.create/create-client wsp))))))

(comment
  (println "hi")
  (-> "journal"
      ;; defworkspace/get-workspace
      ;; workspaces/merge-awm-tags
      toggle-scratchpad)

  (toggle-scratchpad "notes")
  (toggle-scratchpad "web")

  (->>
    (awm/all-clients)
    (map :awesome.client/class))
  )
