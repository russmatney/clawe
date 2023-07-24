(ns doctor.ui.views.git-status
  (:require
   [doctor.ui.db :as ui.db]
   [components.actions :as components.actions]
   [components.git :as components.git]
   [components.table :as components.table]
   [tick.core :as t]
   [uix.core.alpha :as uix]
   [dates.tick :as dates]
   [doctor.ui.handlers :as handlers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bar [{:keys [_conn]}]
  [:div
   {:class ["flex flex-row" "items-center" "justify-between" "whitespace-nowrap"]}
   [:div
    {:class ["text-slate-200" "font-nes" "pr-2"]}
    "Never fetched!"]
   [components.actions/actions-list [] #_(handlers/repo-actions conn)]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-status-table-def []
  {:headers ["Repo" ":dirty?" ":needs-push?" ":needs-pull?" "Actions"]
   :->row   (fn [repo]
              [(components.git/short-repo repo)

               "false"
               "false"
               "false"

               [components.actions/actions-list
                {:actions (handlers/->actions repo)}]])})

(defn repo-table [repos]
  (let [{:keys [->row] :as table-def} (repo-status-table-def)]
    [components.table/table
     (-> table-def
         (assoc :rows (->> repos (map ->row))))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget [{:keys [conn] :as _opts}]
  (let [repos (ui.db/repos conn)]
    [:div
     {:class ["text-center" "my-36" "text-slate-200"]}

     [:div {:class ["font-nes"]} "Never fetched!"]
     [:div {:class ["font-mono"]} "Are you this out of sync?"]

     [repo-table repos]]))
