(ns doctor.ui.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[clawe.workspaces :as clawe.workspaces]
             [clawe.scratchpad :as scratchpad]
             [clawe.db.core :as db]
             [doctor.api.workspaces :as d.workspaces]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

(defn workspace-name [wsp]
  ((some-fn :workspace/display-name :workspace/title :name) wsp))

#?(:clj
   (comment
     (->>
       (d.workspaces/active-workspaces)
       (map workspace-name)
       )
     ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-workspaces [] (d.workspaces/active-workspaces))
(defstream workspaces-stream [] d.workspaces/*workspaces-stream*)

(defn skip-bar-app? [client]
  (and
    (-> client :awesome.client/focused not)
    (-> client :awesome.client/name #{"tauri/doctor-topbar"
                                      "tauri/doctor-popup"})))

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
  (d.workspaces/update-workspaces))

(defhandler show-workspace [item]
  (->
    item
    ;; :name
    ;; clawe.workspaces/for-name
    clawe.workspaces/merge-awm-tags
    scratchpad/toggle-scratchpad)
  (d.workspaces/update-workspaces))

(defhandler update-workspace [item]
  (clawe.workspaces/update-db-workspace item))

#?(:clj
   (comment
     (def --w (->> (d.workspaces/active-workspaces)
                   (filter (comp #{"clawe"} :workspace/title))
                   first))
     (clawe.workspaces/get-db-workspace --w)
     (update-workspace (-> --w
                           (assoc :workspace/directory "/home/russ/russmatney/doctor")
                           ))

     (def --p )
     (->> (d.workspaces/active-workspaces))

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
         [?e :workspace/title ?workspace-title]])
     ))