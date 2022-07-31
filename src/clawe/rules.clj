(ns clawe.rules
  (:require
   [clojure.edn :as edn]
   [clojure.walk :as w]
   [clojure.string :as string]

   [clawe.workspaces :as workspaces]
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [clawe.doctor :as clawe.doctor]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apply rules
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn apply-rules
  "Runs a :rules/apply hook for all defined clawe/defworkspaces."
  ;; TODO could add other sources to this - defkbd, defcom, all defthings?
  []
  (let [rule-fns
        (->>
          (workspaces/all-workspaces)
          (keep :rules/apply))]
    (notify/notify (str "Applying " (count rule-fns) " Clawe rule(s)"))
    (->> rule-fns
         (map (fn [f] (f)))
         doall)))

(defcom clawe-apply-rules apply-rules)

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
                            (workspaces/all-workspaces-fast)
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

(comment
  (->>
    (workspaces/all-workspaces)
    ;; (keep :rules/apply)
    (take 5)
    (map (fn [w]
           (println w)
           w
           ))
    doall)

  (->> [6 5 4 3]
       (group-by odd?))
  )
