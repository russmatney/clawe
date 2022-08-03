(ns clawe.rules
  "Rules are all about getting workspaces and clients in order."
  (:require
   [clojure.string :as string]

   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]
   [ralphie.zsh :as zsh]

   [clawe.doctor :as clawe.doctor]
   [clawe.workspace :as workspace]
   [clawe.wm :as wm]
   [clawe.config :as clawe.config]))

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

(defn clean-workspaces
  "Closes workspaces with 0 clients."
  ([] (clean-workspaces nil))
  ([_]
   (notify/notify {:subject "Removing empty workspaces"})
   (->>
     (wm/active-workspaces {:include-clients true})
     (remove (comp seq :workspace/clients))
     (map
       (fn [it]
         (when-let [title (:workspace/title it)]
           (try
             (wm/delete-workspace it)
             (notify/notify "Deleted Workspace" title)
             (catch Exception e e
                    (notify/notify "Error deleting tag" e))))))
     doall)))

(defn clean-up-workspaces
  ([] (clean-up-workspaces nil))
  ([_]
   (clean-workspaces) ;; remove empty wsps
   (consolidate-workspaces) ;; move preferred indexes down
   (reset-workspace-indexes))) ;; re-sort indexes (repo wsps move down)


(defn correct-clients-and-workspaces
  "Runs over all open clients, rearranging according to workspace rules.

  Selects clients with callbacks to handle
  "
  []
  (let [workspaces        (->>
                            ;; defs b/c we want to create missing wsps if there are any
                            (wm/workspace-defs)
                            (filter :rules/is-my-client?))
        clients           (awm/all-clients)
        _                 (def --w workspaces)
        _                 (def --c clients)
        wsp-by-client     (->>
                            clients
                            (group-by (fn [c]
                                        (->> workspaces
                                             (filter (fn [w] ((:rules/is-my-client? w) c)))
                                             first))))
        claimed-clients   (dissoc wsp-by-client nil)
        unclaimed-clients (get wsp-by-client nil)
        _                 (notify/notify
                            {:notify/print?  true
                             :notify/subject (str "Found " (count unclaimed-clients) " unclaimed clients")
                             :notify/body    (->> unclaimed-clients
                                                  (map
                                                    #(str (:awesome.client/class %)
                                                          " | "
                                                          (:awesome.client/name %)))
                                                  (string/join ", "))})
        multi-tag-clients
        (->>
          clients
          (filter (fn [c] (> (-> c :awesome.client/tags count) 1)))

          )

        _
        (when (seq multi-tag-clients)
          (notify/notify
            {:notify/print?  true
             :notify/subject (str "Found " (count multi-tag-clients) " clients with multiple tags")
             :notify/body    (->> multi-tag-clients
                                  (map
                                    #(str (:awesome.client/class %)
                                          " | "
                                          (:awesome.client/name %)))
                                  (string/join ", "))}))

        mismatched-wsp-clients
        (->>
          claimed-clients
          (map (fn [[w cs]]
                 (let [w-name (:workspace/title w)]
                   [w
                    (->> cs
                         (remove (fn [c]
                                   (#{w-name}
                                     (-> c :awesome.client/tags first :awesome.tag/name)))))])))
          (into {})
          (remove (comp zero? count second))
          (into {}))]

    (when (or true (seq mismatched-wsp-clients))
      (notify/notify
        {:notify/print?  true
         :notify/subject (str "Found " (count mismatched-wsp-clients) " claimed clients on the wrong tag")
         :notify/body    (->> mismatched-wsp-clients
                              (map
                                (fn [[w cs]]
                                  (->> cs
                                       (map
                                         #(str (:awesome.client/class %)
                                               " | "
                                               (:awesome.client/name %)
                                               " on "
                                               (:workspace/title w)))
                                       (apply str))))
                              (string/join ", "))}))

    (def -mwc mismatched-wsp-clients)
    (log "Correcting mismatched clients")
    (->>
      mismatched-wsp-clients
      (map
        (fn [[w cs]]
          (log (str "Ensuring workspace (vilomah): " (:workspace/title w)))
          ;; TODO should this be a workspace-open function?
          (awm/ensure-tag (:workspace/title w))
          (doall (map
                   #(do
                      (log (str "Moving client to workspace: " (:workspace/title w)))
                      (awm/move-client-to-tag (:awesome.client/window %)
                                              (:workspace/title w)))
                   cs))))
      doall)))
