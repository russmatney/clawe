(ns clawe.scratchpad
  (:require
   [clawe.workspaces.create :as wsp.create]
   [clawe.awesome :as awm]
   [clawe.db.scratchpad :as db.scratchpad]
   [ralphie.notify :as r.notify]
   [ralphie.awesome :as r.awm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focus-scratchpad
  "Passes opts to focus-client ensuring that all other windows are buried/tiled,
  the passed client is floated, put ontop, and centered."
  [client]
  (awm/focus-client
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
          tag                   (-> wsp :awesome/tag)
          clients               (-> wsp :awesome/clients)
          client-classes        (-> wsp
                                    :workspace/scratchpad-classes
                                    (conj (:workspace/scratchpad-class wsp))
                                    (->>
                                      (remove nil?)
                                      (into #{})))
          client                (if (seq client-classes)
                                  (some->> clients (filter (comp client-classes :class)) first)
                                  (some->> clients first))
          rules-fn              (-> wsp :rules/apply)
          client-in-another-wsp (when (and (not client) (seq client-classes))
                                  ;; TODO refactor in a workspace client-predicate pattern
                                  ;; (rather than matching on a class like this)
                                  ;; could even just have the workspace-pred-fn return the client directly
                                  (->> (r.awm/all-clients)
                                       (filter (comp client-classes :class))
                                       first))
          centerwork?           (= (:layout tag) "centerwork")
          is-master?            (:master client)]
      (cond
        ;; "found selected tag, client for:" wsp-name
        (and tag client (:selected tag))
        (if (or (:ontop client) (and centerwork? is-master?))
          (do
            ;; hide this tag
            (r.awm/toggle-tag wsp-name)

            ;; restore last buried client
            (let [to-restore (db.scratchpad/next-restore)]
              (when (and to-restore
                         (awm/client-on-tag?
                           to-restore
                           (awm/awm-fnl
                             {:quiet? true}
                             '(-> (awful.screen.focused)
                                  (. :selected_tags)
                                  (lume.map (fn [t] {:name t.name}))
                                  (lume.first)
                                  (. :name)))))
                (awm/focus-client
                  {:bury-all? true
                   :float?    true
                   :center?   false}
                  client)

                (db.scratchpad/mark-restored to-restore))))
          (awm/focus-client
            {:bury-all? true
             :float?    true
             :center?   false}
            client))

        ;; "found unselected tag, client for:" wsp-name
        (and tag client (not (:selected tag)))
        (do
          (r.awm/toggle-tag wsp-name)

          (awm/focus-client
            {:bury-all? true
             :float?    true
             :center?   false}
            client))

        ;; apply rules if another client was found
        (and rules-fn client-in-another-wsp)
        (rules-fn)

        ;; tag exists, no client
        (and tag (not client))
        ;; create the client
        (wsp.create/create-client wsp)

        ;; tag does not exist, presumably no client either
        (not tag)
        (do
          (r.awm/create-tag! wsp-name)
          (r.awm/toggle-tag wsp-name)
          (wsp.create/create-client wsp))))))

(comment
  (println "hi")
  (-> "journal"
      ;; defs.workspaces/get-workspace
      ;; workspaces/merge-awm-tags
      toggle-scratchpad)

  (toggle-scratchpad "notes")
  (toggle-scratchpad "web")

  (->>
    (r.awm/all-clients)
    (map :class))
  )
