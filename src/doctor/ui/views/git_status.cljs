(ns doctor.ui.views.git-status
  (:require
   [doctor.ui.db :as ui.db]
   [components.actions :as components.actions]
   [tick.core :as t]
   [uix.core.alpha :as uix]
   [dates.tick :as dates]
   [doctor.ui.handlers :as handlers]))

(defn bar [{:keys [_conn]}]
  [:div
   {:class ["flex flex-row" "items-center" "justify-between" "whitespace-nowrap"]}
   [:div
    {:class ["text-slate-200" "font-nes" "pr-2"]}
    "Never fetched!"]
   [components.actions/actions-list [] #_(handlers/repo-actions conn)]])

(defn widget [_opts]
  ;; latest-fetch-at
  [:div
   {:class ["text-center" "my-36" "text-slate-200"]}
   [:div {:class ["font-nes"]} "Never fetched!"]
   [:div {:class ["font-mono"]} "Are you this out of sync?"]])
