(ns doctor.ui.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[clawe.workspaces :as clawe.workspaces]
             [clawe.scratchpad :as scratchpad]
             [doctor.api.workspaces :as d.workspaces]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

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
