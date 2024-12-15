(ns doctor.ui.hooks.use-workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   [taoensso.telemere :as t]
   #?@(:clj [[api.workspaces :as api.workspaces]
             [api.topbar :as api.topbar]
             [clawe.wm :as wm]
             [clawe.rules :as clawe.rules]]
       :cljs [[wing.core :as w]
              [uix.core :as uix]
              [doctor.ui.hooks.plasma :refer [with-stream with-rpc]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler clean-up-workspaces []
  (t/log! :info "Cleaning up workspaces")
  (clawe.rules/clean-up-workspaces)
  (api.workspaces/push-updated-workspaces)
  (api.topbar/push-topbar-metadata))

#?(:cljs
   (defn actions []
     [{:action/label    "Clean up"
       :action/on-click #(clean-up-workspaces)}]))

(defhandler close-workspaces [w]
  (t/log! :info (str "Closing workspace" (:workspace/title w)))
  (wm/delete-workspace w)
  (api.workspaces/push-updated-workspaces)
  (api.topbar/push-topbar-metadata))

(defhandler focus-workspace [wsp]
  (t/log! :info (str "Focusing wsp" (:workspace/title wsp)))
  (wm/focus-workspace wsp))

(defhandler focus-client [c]
  (t/log! :info (str "Focusing client" (:client/window-title c)))
  (wm/focus-client c))

(defhandler get-active-workspaces [] (api.workspaces/active-workspaces))
(defstream workspaces-stream [] api.workspaces/*workspaces-stream*)

#?(:cljs
   (defn use-workspaces []
     (let [[workspaces set-workspaces] (uix/use-state [])
           handle-resp
           (fn [active-wsps]
             (t/log! {:data active-wsps} "new wspc data")
             (set-workspaces
               (->> active-wsps
                    (w/distinct-by :workspace/title)
                    (sort-by :workspace/index))))]

       (with-stream [] (workspaces-stream) handle-resp)
       (with-rpc [] (get-active-workspaces) handle-resp)

       {:active-clients      (->> workspaces
                                  (filter :workspace/focused)
                                  (mapcat :workspace/clients))
        :all-clients         (->> workspaces
                                  (mapcat :workspace/clients))
        :active-workspaces   workspaces
        :selected-workspaces (->> workspaces (filter :workspace/focused))})))
