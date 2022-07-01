(ns clawe.scratchpad
  (:require
   [ralphie.awesome :as awm]
   [clawe.awesome :as c.awm] ;; DEPRECATED
   [clawe.workspaces :as workspaces]
   [clawe.workspaces.create :as wsp.create]
   [clawe.db.scratchpad :as db.scratchpad]
   [clojure.set :as set]))

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
          client-names          (-> wsp
                                    :workspace/scratchpad-names
                                    (conj (:workspace/scratchpad-name wsp))
                                    (->>
                                      (remove nil?)
                                      (into #{})))
          client-classes        (-> wsp
                                    :workspace/scratchpad-classes
                                    (conj (:workspace/scratchpad-class wsp))
                                    (->>
                                      (remove nil?)
                                      (into #{})))
          client                (if (seq client-classes)
                                  (some->> clients (filter
                                                     #(or
                                                        ((comp client-classes :awesome.client/class) %)
                                                        ((comp client-names :awesome.client/name) %))
                                                     ) first)
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
        ;; "found selected tag&client for:" wsp-name
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


;; TODO move to pure input/output, return a description of the actions to take (some event type?)
(defn toggle-scratchpad-2
  "Creates and focuses the passed scratchpad.
  If it exists anywhere, it will be found and pulled into the current workspace.
    (this includes 'correcting' the scratchpad's tag if it is wrong)
  If it is already in the current workspace but not focused, it will be focused.
  If it is in focus, it will be removed (hidden).
  Once removed, a previously focused, just-buried scratchpad will have its focus restored.
  "
  ;; TODO the scratchpad passed may not exist yet in awesome
  ;; TODO create tag if it doesn't exist
  ([scratchpad-wsp]
   (toggle-scratchpad-2
     scratchpad-wsp
     {:current-workspaces (workspaces/current-workspaces)
      ;; TODO move to workspaces/all-clients ? something generic?
      :all-clients (awm/all-clients)}))

  ([scratchpad-wsp {:keys [current-workspaces all-clients]}]
   (let [current-clients (->> current-workspaces
                              (map :awesome.tag/clients)
                              (apply concat))

         is-scratchpad-client? (:scratchpad/is-my-client? scratchpad-wsp)

         scratchpad-clients (->> all-clients
                                 (filter is-scratchpad-client?))

         on-wrong-tag? (->> scratchpad-clients
                            (filter (comp
                                      seq
                                      #(set/difference
                                         #{(:awesome.tag/name scratchpad-wsp)})
                                      :awesome.client/tag-names)))

         scratchpad-client-current (some->> current-clients
                                            (filter is-scratchpad-client?)
                                            first)

         is-focused? (:awesome.client/focused scratchpad-client-current)

         scratchpad-client-current-is-on-scratchpad-tag?
         (-> scratchpad-client-current
             :awesome.client/tag-names
             ;; TODO clients should generally be on one tag only
             ;; (but this may change)
             first
             #{(:awesome.tag/name scratchpad-wsp)})

         ]

     ;; client exists, but is on the wrong tag - lets correct that
     (when (and scratchpad-clients on-wrong-tag?)
       (->> scratchpad-clients
            (map
              ;; TODO create awm tag if it doesn't exist
              #(awm/move-client-to-tag (:awesome.client/window %)
                                       (:awesome.tag/name scratchpad-wsp)))))

     (cond
       (and scratchpad-client-current is-focused?)
       ;; hide the scratchpad tag
       (awm/toggle-tag (:awesome.tag/name scratchpad-wsp))

       (and scratchpad-client-current (not is-focused?))
       ;; focus/center the scratchpad tag
       (awm/toggle-tag (:awesome.tag/name scratchpad-wsp))

       :else
       (do
         ;; TODO nothing
         )
       )
     )
   ))

(comment
  (->>
    (workspaces/all-workspaces)
    (filter :awesome.tag/selected)
    ;; (filter :workspace/scratchpad)
    first)

  (toggle-scratchpad-2 nil)
  )
