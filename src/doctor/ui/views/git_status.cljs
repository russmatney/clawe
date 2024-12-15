(ns doctor.ui.views.git-status
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.actions :as components.actions]
   [components.git :as components.git]
   [components.table :as components.table]
   [components.debug :as components.debug]
   [dates.tick :as dates]
   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]))

(defn needs-push? [{:repo/keys [needs-push-at did-not-need-push-at]}]
  (dates/newer needs-push-at did-not-need-push-at))

(defn dirty? [{:repo/keys [dirty-at clean-at]}]
  (dates/newer dirty-at clean-at))

(defn needs-pull? [{:repo/keys [needs-pull-at did-not-need-pull-at]}]
  (dates/newer needs-pull-at did-not-need-pull-at))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-status-label [repo]
  (str (:repo/name repo)
       (when (dirty? repo) "#dirty")
       (when (needs-pull? repo) "#needs-pull")
       (when (needs-push? repo) "#needs-push")))

(defui bar [{:keys [conn]}]
  (let [repos (->> (ui.db/repos conn)
                   (sort dirty?)
                   (sort needs-pull?)
                   (sort needs-push?))
        axs   (->> repos
                   (map (fn [repo]
                          {:action/label    (repo-status-label repo)
                           :action/on-click #(handlers/check-repo-status repo)
                           ;; TODO disable/de-proritize if recently checked
                           :action/disabled false})))]

    ($ components.actions/actions-list axs)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-status-table-def []
  {:headers ["Repo" ":dirty?" ":needs-push?" ":needs-pull?" "Actions"]
   :->row   (fn [repo]
              [($ components.debug/raw-data
                  {:label (components.git/short-repo repo)
                   :data  repo})

               (when (dirty? repo) "DIRTY!?")
               (when (needs-push? repo) "Needs Push!?")
               (when (needs-pull? repo) "Needs Pull!?")

               ($ components.actions/actions-list
                  {:actions (handlers/->actions repo)})])})

(defui repo-table [{:keys [repos]}]
  (let [{:keys [->row] :as table-def} (repo-status-table-def)]
    ($ components.table/table
       (-> table-def
           (assoc :rows (->> repos (map ->row)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui widget [{:keys [conn] :as _opts}]
  (let [repos (->> (ui.db/watched-repos conn)
                   (sort dirty?)
                   (sort needs-pull?)
                   (sort needs-push?))]
    ($ :div
       {:class ["text-center" "my-36" "text-slate-200"]}

       (when (empty? repos)
         ($ :div {:class ["font-nes"]} "No repos found!"))

       ($ repo-table {:repos repos}))))
