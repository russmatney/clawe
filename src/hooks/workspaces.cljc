(ns hooks.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[clawe.workspaces :as clawe.workspaces]
             [clawe.scratchpad :as scratchpad]
             [defthing.db :as db]
             [defthing.defworkspace :as defworkspace]
             [api.workspaces]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

(defn workspace-name [wsp]
  ((some-fn :workspace/display-name :workspace/title :name) wsp))

#?(:clj
   (comment
     (->>
       (api.workspaces/active-workspaces)
       (map workspace-name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-workspaces [] (api.workspaces/active-workspaces))
(defstream workspaces-stream [] api.workspaces/*workspaces-stream*)

(defn skip-bar-app? [client]
  (and
    (-> client :awesome.client/focused not)
    (-> client :awesome.client/name #{"tauri/doctor-topbar"})))

#?(:cljs
   (defn use-workspaces []
     (let [workspaces  (plasma.uix/state [])
           handle-resp (fn [new-wsps]
                         (swap! workspaces
                                (fn [_wsps]
                                  (->> new-wsps
                                       (w/distinct-by :workspace/title)
                                       (sort-by :awesome.tag/index)))))]

       (with-rpc [] (get-workspaces) handle-resp)
       (with-stream [] (workspaces-stream) handle-resp)

       {:workspaces        @workspaces
        :active-clients    (->> @workspaces
                                (filter :awesome.tag/selected)
                                (mapcat :awesome.tag/clients)
                                (remove skip-bar-app?)
                                (w/distinct-by :awesome.client/window))
        :active-workspaces (->> @workspaces (filter :awesome.tag/selected))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler hide-workspace [item]
  (->
    ;; TODO support non scratchpad workspaces - could be a quick awm-fnl show-only
    item
    ;; :name
    ;; clawe.workspaces/for-name
    clawe.workspaces/merge-awm-tags
    scratchpad/toggle-scratchpad)
  (api.workspaces/update-workspaces))

(defhandler show-workspace [item]
  (->
    item
    ;; :name
    ;; clawe.workspaces/for-name
    clawe.workspaces/merge-awm-tags
    scratchpad/toggle-scratchpad)
  (api.workspaces/update-workspaces))

(defhandler update-workspace [item]
  (defworkspace/sync-workspaces-to-db item))

#?(:clj
   (comment
     (def --w (->> (api.workspaces/active-workspaces)
                   (filter (comp #{"clawe"} :workspace/title))
                   first))
     (defworkspace/get-db-workspace --w)
     (update-workspace (-> --w
                           (assoc :workspace/directory "/home/russ/russmatney/doctor")
                           ))

     (def --p )
     (->> (api.workspaces/active-workspaces))

     (update-workspace (-> --w
                           (assoc :workspace/display-name "ClAwE")
                           ))

     (db/query
       '[:find (pull ?e [*])
         :in $ ?workspace-title
         :where
         [?e :workspace/title ?workspace-title]]
       "clawe")

     (db/query
       '[:find (pull ?e [*])
         :where
         [?e :workspace/title ?workspace-title]])))
