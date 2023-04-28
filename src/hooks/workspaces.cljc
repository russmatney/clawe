(ns hooks.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.workspaces :as api.workspaces]
             [api.topbar :as api.topbar]
             [clawe.wm :as wm]
             [clawe.rules :as clawe.rules]
             [taoensso.timbre :as log]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler clean-up-workspaces []
  (log/info "Cleaning up workspaces")
  (clawe.rules/clean-up-workspaces)
  (api.workspaces/push-updated-workspaces)
  (api.topbar/push-topbar-metadata))

#?(:cljs
   (defn actions []
     [{:action/label    "Clean up"
       :action/on-click #(clean-up-workspaces)}]))

(defhandler close-workspaces [w]
  (log/info "Closing workspace" (:workspace/title w))
  (wm/delete-workspace w)
  (api.workspaces/push-updated-workspaces)
  (api.topbar/push-topbar-metadata))

(defhandler focus-workspace [wsp]
  (log/info "Focusing wsp" (:workspace/title wsp))
  (wm/focus-workspace wsp))

(defhandler focus-client [c]
  (log/info "Focusing client" (:client/window-title c))
  (wm/focus-client c))

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
