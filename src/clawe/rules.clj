(ns clawe.rules
  (:require
   [clojure.string :as string]

   [clawe.workspaces :as workspaces]
   [clawe.workspace :as workspace]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [clawe.doctor :as clawe.doctor]))

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

(defn correct-clients-and-workspaces
  "Runs over all open clients, rearranging according to workspace rules.

  Selects clients with callbacks to handle
  "
  []
  (let [workspaces        (->>
                            ;; defs b/c we want to create missing wsps if there are any
                            (workspace/all-defs)
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
      doall)

    (log "Cleaning, consolidating, and updating indexes")
    (workspaces/clean-workspaces)
    (workspaces/consolidate-workspaces)
    (workspaces/update-workspace-indexes)
    (clawe.doctor/update-topbar)))
