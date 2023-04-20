(ns hooks.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.workspaces]
             [clawe.wm :as wm]
             [taoensso.timbre :as log]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler close-workspaces [w]
  (log/info "Closing workspace" (:workspace/title w))
  (wm/delete-workspace w))

(defhandler get-active-workspaces [] (api.workspaces/active-workspaces))
(defstream workspaces-stream [] api.workspaces/*workspaces-stream*)

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
                                  (filter :workspace/focused)
                                  (mapcat :workspace/clients))
        :all-clients         (->> @workspaces
                                  (mapcat :workspace/clients))
        :active-workspaces   @workspaces
        :selected-workspaces (->> @workspaces (filter :workspace/focused))})))
