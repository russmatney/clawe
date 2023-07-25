(ns clawe.rules
  "Rules are all about getting workspaces and clients in order."
  (:require
   [ralphie.notify :as notify]

   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.workspace :as workspace]
   [clawe.client :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reset workspace indexes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- wsp-sort-key
  "Returns a sort key for workspaces.

  If the workspace's directory is the home dir, it will by prefixed by a 'z',
  so that it ends up with a higher index when sorted."
  [wsp]
  (str (if (#{(clawe.config/home-dir)} (:workspace/directory wsp)) "z" "a") "-"
       ;; TODO better to opt-in via sort-front/sort-back options
       ;; case: send journal to back, bring some repos to front
       (cond
         ;; sort preferred ahead of reusing index
         (:workspace/preferred-index wsp)
         (format "%03d" (:workspace/preferred-index wsp))

         :else
         (format "%02d" (or (:workspace/index wsp) 0)))))

(defn- workspaces-to-swap-indexes
  "Creates a sorted list of workspaces with an additional key: :new-index.
  This is in support of the `sort-workspace-indexes` func below."
  []
  (->> (wm/active-workspaces)
       (map (fn [wsp] (assoc wsp :sort-key (wsp-sort-key wsp))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp]
                      (assoc wsp :new-index
                             (case (clawe.config/get-wm)
                               :wm/i3 i
                               ;; lua indexes start at 1, and osx's first wsp as 1...
                               ;; this is probably right for most wm indexes (to match the keyboard)
                               (+ i 1)))))
       (remove #(= (:new-index %) (:workspace/index %)))))

(comment
  (workspaces-to-swap-indexes)
  )

(defn sort-workspace-indexes
  "Re-sorts workspace indexes according to the sort key in `wsp-sort-key`,
  which generally moves repo-workspaces down and scratchpad-indexes up.

  The scratchpad indexes tend to have alternate bindings for toggling,
  so they don't need to be taking up the useful 0-9 bindings
  when many workspaces are open."
  ([] (sort-workspace-indexes nil))
  ([_]
   (notify/notify {:subject "Sorting workspaces"})
   (loop [wsps    (workspaces-to-swap-indexes)
          last-ct nil]
     (if (and last-ct (#{last-ct} (count wsps)))
       (do
         (notify/notify {:subject "Error sorting workspaces"})
         [:error "Sorting workspaces fail"])
       (let [wsp (some-> wsps first)]
         (when wsp
           (let [{:keys [new-index]} wsp
                 index               (-> wsp :workspace/title
                                         wm/fetch-workspace :workspace/index)]
             (when (not= new-index index)
               (wm/swap-workspaces-by-index index new-index))
             ;; could be optimized....
             (recur (workspaces-to-swap-indexes) (count wsps)))))))))

(comment
  (sort-workspace-indexes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces
  "Updates workspaces such that there are no gaps in the indexes, and the indexes start at 1."
  ([] (consolidate-workspaces nil))
  ([_]
   (notify/notify {:subject "Consolidating not-empty workspaces"})
   (->>
     (wm/active-workspaces {:include-clients true})
     (remove (comp seq :workspace/clients))
     (sort-by :workspace/index)
     (map-indexed
       (fn [new-index {:keys [workspace/title workspace/index]}]
         (let [new-index (+ 1 new-index)] ;; b/c lua is 1-based
           (if (= index new-index)
             (prn "nothing to do")
             (do
               (prn "swapping workspaces"
                    {:title     title
                     :idx       index
                     :new-index new-index})
               (wm/swap-workspaces-by-index index new-index)))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-bar-app? [client]
  (-> client :client/window-title #{"tauri-doctor-topbar"}))

(defn clean-workspaces
  "Closes workspaces with 0 clients or nil titles."
  ([] (clean-workspaces nil))
  ([last]
   (notify/notify {:subject "Removing empty or title-less workspaces"})
   (let [wsps           (wm/active-workspaces {:include-clients true})
         many-to-delete (->> wsps
                             (filter (fn [wsp]
                                       (or
                                         (-> wsp :workspace/title nil?)
                                         (->> wsp :workspace/clients
                                              (remove is-bar-app?)
                                              seq empty?)))))
         it             (some-> many-to-delete first)]
     (cond
       (and (= 1 (count wsps))
            (seq many-to-delete))
       (notify/notify "Refusing to delete last workspace")

       (and (not (nil? it)) (= last it))
       (notify/notify "Error deleting workspaces")

       it
       (try
         (println "Deleting workspace" (workspace/strip it))
         (wm/delete-workspace it)
         (notify/notify {:notify/subject "Deleted Workspace"
                         :notify/body    (:workspace/title it "no-title")})

         ;; call again until there are none left?
         (clean-workspaces it)
         (catch Exception e e
                (notify/notify {:notify/subject "Error deleting workspace"
                                :notify/body    e})))

       :else (notify/notify "Finished deleting workspaces")))))

(comment
  (clean-workspaces))

(defn return-clients-to-expected-workspaces
  "Runs over all open clients, moving them to their expected workspace."
  ([] (return-clients-to-expected-workspaces nil))
  ([opts]
   (let [clients    (wm/active-clients)
         workspaces (wm/active-workspaces {:prefetched-clients clients})

         ;; TODO consider branching for opt-in :scratchpads here (vs creating on-the-fly workspaces)
         corrections
         (->> clients
              (map (fn [client]
                     (if-let [wsp (some->> workspaces
                                           (filter #(workspace/find-matching-client % client))
                                           first)]
                       (assoc client :client/workspace wsp)
                       client)))
              ;; assuming all clients have workspaces...and just one....
              (remove (fn [client]
                        (= (wm/client->workspace-title client)
                           (-> client :client/workspace :workspace/title))))
              (map (fn [client]
                     [:move-client-to-wsp client (wm/client->workspace-title client)])))]
     (if (:dry-run opts)
       corrections
       (do
         (notify/notify (str "Found " (count corrections) " corrections to apply"))
         (doall
           (->> corrections
                (map (fn [[action & rest]]
                       (cond
                         (= :move-client-to-wsp action)
                         (let [[client wsp] rest]
                           (wm/move-client-to-workspace client wsp))

                         :else
                         (println "Unsupported correction" action)))))))))))

(comment
  (clawe.config/reload-config)
  (return-clients-to-expected-workspaces)
  (->>
    (wm/active-clients)
    (map client/strip)
    (filter (comp #{"godot"} :client/app-name)))
  (return-clients-to-expected-workspaces {:dry-run true}))

(defn clean-up-workspaces
  ([] (clean-up-workspaces nil))
  ([_]
   (return-clients-to-expected-workspaces)
   (clean-workspaces) ;; remove empty wsps
   (consolidate-workspaces) ;; move preferred indexes down
   (sort-workspace-indexes) ;; re-sort indexes (repo wsps move down)
   (clawe.doctor/update-topbar)))
