(ns hooks.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.workspaces]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-active-workspaces [] (api.workspaces/active-workspaces))
(defstream workspaces-stream [] api.workspaces/*workspaces-stream*)

(defn skip-bar-app? [client]
  (and
    (-> client :awesome.client/focused not)
    (-> client :awesome.client/name #{"tauri/doctor-topbar"})))

#?(:cljs
   (defn use-workspaces []
     (let [workspaces  (plasma.uix/state [])
           handle-resp (fn [active-wsps]
                         (swap! workspaces
                                (fn [_wsps]
                                  (->> active-wsps
                                       (w/distinct-by :workspace/title)
                                       (sort-by :workspace/index)))))]

       (with-rpc [] (get-active-workspaces) handle-resp)
       (with-stream [] (workspaces-stream) handle-resp)

       {:active-clients      (->> @workspaces
                                  (filter :awesome.tag/selected)
                                  (mapcat :awesome.tag/clients)
                                  (remove skip-bar-app?)
                                  (w/distinct-by :awesome.client/window))
        :active-workspaces   @workspaces
        :selected-workspaces (->> @workspaces (filter :awesome.tag/selected))})))
