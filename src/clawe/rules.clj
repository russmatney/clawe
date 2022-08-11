(ns clawe.rules
  "Rules are all about getting workspaces and clients in order."
  (:require
   [ralphie.notify :as notify]

   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.workspace :as workspace]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Correct clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn log
  ([subj] (log subj nil))
  ([subj body]
   (notify/notify (cond-> {:notify/print?  true
                           :notify/subject subj
                           :notify/id      "correcting-with-rules"}
                    body
                    (assoc :notify/body body)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reset workspace indexes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- wsp-sort-key [wsp]
  (str (if (#{(clawe.config/home-dir)} (:workspace/directory wsp)) "z" "a") "-"
       (format "%03d" (or (:workspace/index wsp) 0))))

(defn- workspaces-to-swap-indexes
  "Creates a sorted list of workspaces with an additional key: :new-index.
  This is in support of the `reset-workspace-indexes` func below."
  []
  (->> (wm/active-workspaces)
       (map (fn [wsp] (assoc wsp :sort-key (wsp-sort-key wsp))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp]
                      ;; lua indexes start at 1, and osx's first wsp as 1...
                      ;; this is probably right for most wm indexes (to match the keyboard)
                      (assoc wsp :new-index (+ i 1))))
       (remove #(= (:new-index %) (:workspace/index %)))))

(defn reset-workspace-indexes
  ([] (reset-workspace-indexes nil))
  ([_]
   (notify/notify {:subject "Resetting workspace indexes"})
   (loop [wsps (workspaces-to-swap-indexes)]
     (let [wsp (some-> wsps first)]
       (when wsp
         (let [{:keys [new-index]} wsp
               index               (-> wsp :workspace/title
                                       wm/fetch-workspace :workspace/index)]
           (when (not= new-index index)
             (wm/swap-workspaces-by-index index new-index))
           ;; could be optimized....
           (recur (workspaces-to-swap-indexes))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces
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
               (prn "swapping tags" {:title     title
                                     :idx       index
                                     :new-index new-index})
               (wm/swap-workspaces-by-index index new-index)))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-bar-app? [client]
  (-> client :client/window-title #{"tauri/doctor-topbar"}))

(defn clean-workspaces
  "Closes workspaces with 0 clients."
  ([] (clean-workspaces nil))
  ([_]
   (notify/notify {:subject "Removing empty or title-less workspaces"})
   (->>
     (wm/active-workspaces {:include-clients true})
     (filter (fn [wsp]
               (or
                 (-> wsp :workspace/title nil?)
                 (->> wsp :workspace/clients
                      (remove is-bar-app?)
                      seq empty?))))
     (map
       (fn [it]
         (when-let [title (:workspace/title it)]
           (try
             (println "Deleting workspace" it)
             (wm/delete-workspace it)
             (notify/notify "Deleted Workspace" title)
             (catch Exception e e
                    (notify/notify "Error deleting tag" e))))))
     doall)))

(defn correct-clients-and-workspaces
  "Runs over all open clients, rearranging according to workspace rules.

  Selects clients with callbacks to handle
  "

  ([] (correct-clients-and-workspaces nil))
  ([opts]
   (let [clients    (wm/active-clients)
         workspaces (wm/active-workspaces {:prefetched-clients clients})
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
  (correct-clients-and-workspaces)
  (correct-clients-and-workspaces {:dry-run true}))

(defn clean-up-workspaces
  ([] (clean-up-workspaces nil))
  ([_]
   (correct-clients-and-workspaces)
   (clean-workspaces) ;; remove empty wsps
   (consolidate-workspaces) ;; move preferred indexes down
   (reset-workspace-indexes)
   (clawe.doctor/update-topbar))) ;; re-sort indexes (repo wsps move down)
