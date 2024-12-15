(ns doctor.ui.views.workspaces
  (:require
   [clojure.string :as string]
   [taoensso.telemere :as log]
   [uix.core :as uix :refer [$ defui]]

   [components.icons :as icons]
   [components.debug :as debug]
   [components.actions :as actions]
   [components.clients :as clients]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.hooks.use-workspaces :as hooks.use-workspaces]
   [doctor.ui.hooks.use-topbar :as hooks.use-topbar]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.views.git-status :as git-status]
   [dates.tick :as dates]))

(defn dir [s]
  (-> s
      (string/replace #"/Users/russ" "~")
      (string/replace #"/home/russ" "~")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Detail window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui client-detail
  [{:keys [client] :as opts}]
  (let [{:client/keys [app-name window-title]} client]
    ($ :div
       {:class ["flex" "flex-col" "mb-3" "mr-3" "p-3"
                "rounded" "border" "border-city-green-400"
                "bg-emerald-900"
                "w-96"]}

       ($ :div.mb-4
          {:class ["flex" "flex-row" "items-center"]}
          ($ icons/icon-comp
             (assoc (icons/client->icon client nil)
                    :class ["w-8" "mr-4"]))

          ($ :span.text-xl
             (string/lower-case
               (str window-title " | " app-name)))

          ($ :div
             {:class ["ml-auto"]}
             ($ debug/raw-metadata
                (merge {:label "RAW"
                        :data  (->> client (sort-by first))}
                       opts))))

       ($ :div
          {:class ["flex" "flex-row" "items-center"]}

          ($ :div
             {:class ["ml-auto"]}
             ($ actions/actions-list {:actions (handlers/->actions client)}))))))

(defui active-workspace [{:keys [workspaces]}]
  ($ :div
     {:class ["bg-yo-blue-500"
              "m-2" "p-6"
              "border" "rounded"
              "text-white" "w-full"
              "flex flex-col"]}
     (when (seq workspaces)
       (for [[i wsp] (->> workspaces (map-indexed vector))]
         (let [{:keys [ ;; TODO restore these git features
                       git/needs-push?
                       git/dirty?
                       git/needs-pull?
                       ;; TODO restore this tmux feat
                       tmux/session
                       workspace/directory
                       workspace/title
                       workspace/clients]} wsp]
           ($ :div
              {:key   i
               :class ["flex flex-col"]}
              ($ :div
                 {:class ["flex flex-row justify-between items-center"]}
                 ($ :span {:class ["text-xl font-nes"]}
                    (:workspace/title wsp))

                 ($ :span {:class ["ml-auto"]}
                    (str (when needs-push? "#needs-push")
                         (when needs-pull? "#needs-pull")
                         (when dirty? "#dirty")))

                 ($ :span {:class ["ml-auto"]}
                    ($ actions/actions-list {:actions (handlers/->actions wsp)})))

              ($ :div
                 {:class ["mb-4" "font-mono"]}
                 (dir directory))

              (when session
                ($ debug/raw-metadata
                   {:label "Tmux Metadata" :data session}))

              ($ :div
                 {:class ["flex flex-row justify-between items-center" "py-2"]}
                 ($ :span {:class ["text-lg font-mono"]}
                    (str (count clients) " client(s)")))

              (when (seq clients)
                ($ :div
                   {:class ["flex flex-row flex-wrap" "items-center"]}
                   (for [client clients]
                     ($ client-detail {:key    (:client/key client (:client/window-title client))
                                       :client client}))))

              ($ :div
                 {:class ["ml-auto"]}
                 ($ debug/raw-metadata
                    {:label "RAW" :data wsp}))))))))

(defui topbar-metadata []
  (let [metadata (hooks.use-topbar/use-topbar-metadata)]
    ($ :span
       {:class ["text-slate-200"]}
       ($ debug/raw-metadata
          {:label "Topbar Metadata"
           :data  (some->> metadata (sort-by first))}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo-comp

(defui repo-comp
  [{:keys [item index] :as _opts}]
  (let [repo item]
    ($ :div
       {:class ["m-1" "p-4" "w-96"
                "border" "rounded"
                "border-city-blue-600"
                "bg-yo-blue-700"
                "text-slate-600"]}
       ($ :div
          {:class ["flex flex-row" "items-center"]}
          ($ :div {:class ["font-nes"]}
             (str "(" index ") " (:repo/name repo)))

          ($ :span {:class ["ml-auto"]}
             ($ actions/actions-list {:actions (handlers/->actions repo)})))

       ($ :div
          {:class ["flex" "flex-col"]}
          (when (and (nil? (:repo/clean-at repo))
                     (nil? (:repo/dirty-at repo)))
            ($ :div {:class ["text-slate-800"]}
               (str "never checked")))

          (when (or (:repo/clean-at repo)
                    (:repo/dirty-at repo))
            ($ :div
               {:class [(when (git-status/dirty? repo) "text-city-red-700")]}
               (if (git-status/dirty? repo) "DIRTY!?"
                   (str "clean " (dates/human-time-since (:repo/clean-at repo)) " ago"))))
          (when (:repo/did-not-need-push-at repo)
            ($ :div
               {:class [(when (git-status/needs-push? repo) "text-city-red-700")]}
               (if (git-status/needs-push? repo) "Needs Push!?"
                   (str "did-not-need-push "
                        (dates/human-time-since (:repo/did-not-need-push-at repo))
                        " ago"))))
          (when (:repo/did-not-need-pull-at repo)
            ($ :div
               {:class [(when (git-status/needs-pull? repo) "text-city-red-700")]}
               (if (git-status/needs-pull? repo) "Needs Pull!?"
                   (str "did-not-need-pull-at "
                        (dates/human-time-since (:repo/did-not-need-pull-at repo))
                        " ago")))))

       (debug/raw-metadata {:data repo})
       )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui workspace-comp
  [{:keys [workspace] :as _opts}]
  (let [{:keys [workspace/directory
                workspace/color
                workspace/title-hiccup
                workspace/index
                workspace/clients]} workspace]
    ($ :div
       {:class ["m-1"
                "p-4"
                "border"
                "rounded"
                "border-city-blue-600"
                "bg-yo-blue-700"
                "text-white"
                "w-96"
                ]}
       ($ :div
          {:class ["flex flex-row" "items-center"]}
          ($ :div
             (merge
               (when color {:style {:color color}})
               {:class ["font-nes"]})
             (str "(" index ") " (:workspace/title workspace)))

          ($ :span {:class ["ml-auto"]}
             ($ actions/actions-list {:actions (handlers/->actions workspace)})))

       (when (seq clients)
         ($ components.clients/client-icon-list
            {:clients clients :workspace workspace}))

       (when (seq clients)
         ($ :ul
            {:class ["truncate"]}
            (for [[i c] (->> clients (map-indexed vector))]
              ($ :li
                 {:key   i
                  :class ["flex flex-row" "items-center"]}
                 (str (:client/window-title c) " | "
                      (:client/app-name c))
                 ($ :span {:class ["ml-auto"]}
                    ($ actions/actions-popup
                       {:comp
                        ($ :span {:class ["font-nes" "text-xs"]} "axs")
                        :actions
                        (handlers/->actions c)}))))))

       (when title-hiccup
         ($ :div title-hiccup))

       (when directory
         ($ :span
            {:class ["font-mono"]}
            (dir directory))))))

(defui widget [_opts]
  (let [{:keys [selected-workspaces active-workspaces]}
        (hooks.use-workspaces/use-workspaces)

        [show-current? set-show-current] (uix/use-state false)

        {:keys [data]} (hooks.use-db/use-query
                         {:q '[:find (pull ?e [*])
                               :where
                               [?e :doctor/type :type/repo]]})

        repos (->> data
                   (sort git-status/dirty?)
                   (sort git-status/needs-pull?)
                   (sort git-status/needs-push?))]
    (log/log! {:data {:selected (count selected-workspaces)
                      :active   (count active-workspaces)
                      :repos    (count repos)}}
              "workspaces widget rendering")
    ($ :div
       {:class ["p-4"]}
       ($ :div.header
          {:class ["flex flex-row" "items-center"]}
          ($ :h1
             {:class ["font-nes" "text-2xl" "text-white" "pb-2"]}
             (str "Workspaces (" (count active-workspaces) ")"))

          ($ actions/actions-list
             {:actions
              [{:action/on-click #(set-show-current not)
                :action/label
                (if show-current? "hide current" "show current")}]})

          ($ :div
             {:class ["ml-auto"]}
             ($ topbar-metadata)))

       ($ :div.content
          {:class ["flex" "flex-row" "pt-4"]}
          ($ :div
             {:class ["flex" "flex-0"
                      (if show-current? "flex-col" "flex-row")
                      "flex-wrap"
                      "justify-center"
                      ]}
             (for [[i it] (->> active-workspaces (map-indexed vector))]
               ($ workspace-comp {:key i :workspace it}))

             (for [[i it] (->> repos

                               (map-indexed vector))]
               ($ repo-comp {:key i :index i :item it})))

          (when show-current?
            ($ :div
               {:class ["flex" "flex-1"]}
               ($ active-workspace {:workspaces selected-workspaces})))))))
