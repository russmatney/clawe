(ns pages.workspaces
  (:require
   [clojure.string :as string]
   [hooks.workspaces :as hooks.workspaces]
   [hooks.topbar :as hooks.topbar]
   [doctor.ui.handlers :as handlers]
   [components.icons :as icons]
   [components.debug :as debug]
   [components.actions :as actions]))

(defn dir [s]
  (-> s
      (string/replace #"/Users/russ" "~")
      (string/replace #"/home/russ" "~")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Detail window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-detail
  ([client] [client-detail nil client])
  ([opts client]
   (let [{:client/keys [app-name window-title]} client]
     [:div
      {:class ["flex" "flex-col" "mb-3" "mr-3" "p-3"
               "rounded" "border" "border-city-green-400"
               "bg-emerald-900"
               "w-96"]}

      [:div.mb-4
       {:class ["flex" "flex-row" "items-center"]}
       [icons/icon-comp
        (assoc (icons/client->icon client nil)
               :class ["w-8" "mr-4"])]

       [:span.text-xl
        (string/lower-case
          (str window-title " | " app-name))]

       [:div
        {:class ["ml-auto"]}
        [debug/raw-metadata
         (merge {:label "RAW"} opts)
         (->> client (sort-by first))]]]

      [:div
       {:class ["flex" "flex-row" "items-center"]}

       [:div
        {:class ["ml-auto"]}
        [actions/actions-list (handlers/->actions client)]]]])))

(defn active-workspace [active-workspaces]
  [:div
   {:class ["bg-yo-blue-500"
            "m-2" "p-6"
            "border" "rounded"
            "text-white" "w-full"
            "flex flex-col"]}
   (when (seq active-workspaces)
     (for [wsp active-workspaces]
       (let [{:keys [;; TODO restore these git features
                     git/needs-push?
                     git/dirty?
                     git/needs-pull?
                     workspace/directory
                     workspace/title
                     workspace/clients
                     tmux/session]} wsp]

         ^{:key title}
         [:div
          {:class ["flex flex-col"]}
          [:div
           {:class ["flex flex-row justify-between items-center"]}
           [:span {:class ["text-xl font-nes"]}
            (:workspace/title wsp)]

           [:span {:class ["ml-auto"]}
            (str (when needs-push? "#needs-push")
                 (when needs-pull? "#needs-pull")
                 (when dirty? "#dirty"))]

           [:span {:class ["ml-auto"]}
            [actions/actions-list (handlers/->actions wsp)]]]

          [:div
           {:class ["mb-4" "font-mono"]}
           (dir directory)]

          (when session
            [debug/raw-metadata
             {:label "Tmux Metadata"}
             session])

          [:div
           {:class ["flex flex-row justify-between items-center" "py-2"]}
           [:span {:class ["text-lg font-mono"]}
            (str (count clients) " client(s)")]]

          (when (seq clients)
            [:div
             {:class ["flex flex-row flex-wrap" "items-center"]}
             (for [client clients]
               ^{:key (:client/key client (:client/window-title client))}
               [client-detail client])])

          [:div
           {:class ["ml-auto"]}
           [debug/raw-metadata
            {:label "RAW"}
            wsp]]])))])

(defn topbar-metadata []
  (let [metadata (hooks.topbar/use-topbar-metadata)]
    [:span
     {:class ["text-slate-200"]}
     [debug/raw-metadata
      {:label "Topbar Metadata"}
      (some->> @metadata (sort-by first))]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-comp
  ([wsp] (workspace-comp nil wsp))
  ([_opts wsp]
   (let [{:keys [workspace/directory
                 workspace/color
                 workspace/title-hiccup
                 workspace/index
                 workspace/clients]} wsp]
     [:div
      {:class ["m-1"
               "p-4"
               "border"
               "rounded"
               "border-city-blue-600"
               "bg-yo-blue-700"
               "text-white"
               "w-96"
               ]}
      [:div
       {:class ["flex flex-row" "items-center"]}
       [:div
        (merge
          (when color {:style {:color color}})
          {:class ["font-nes"]})
        (str "(" index ") " (:workspace/title wsp))]

       [:span {:class ["ml-auto"]}
        [actions/actions-list (handlers/->actions wsp)]]]

      (when (seq clients)
        [:ul
         {:class ["truncate"]}
         (for [c (->> clients)]
           ^{:key (:client/id c)}
           [:li
            {:class ["flex flex-row" "items-center"]}
            (str (:client/window-title c) " | "
                 (:client/app-name c))
            [:span {:class ["ml-auto"]}
             [actions/actions-popup
              {:comp
               [:span {:class ["font-nes" "text-xs"]} "axs"]
               :actions
               (handlers/->actions c)}]]])])

      (when title-hiccup
        [:div title-hiccup])

      (when directory
        [:span
         {:class ["font-mono"]}
         (dir directory)])])))

(defn page [_opts]
  (let [{:keys [selected-workspaces active-workspaces]} (hooks.workspaces/use-workspaces)]
    [:div
     {:class ["p-4"]}
     [:div
      {:class ["flex flex-row" "items-center"]}
      [:h1
       {:class ["font-nes" "text-2xl" "text-white" "pb-2"]}
       (str "Workspaces (" (count active-workspaces) ")")]

      [:div
       {:class ["ml-auto"]}
       [topbar-metadata]]]

     [:div
      {:class ["flex" "flex-row" "pt-4"]}
      [:div
       {:class ["flex" "flex-0" "flex-col" "flex-wrap" "justify-between"]}
       (for [[i it] (->> active-workspaces (map-indexed vector))]
         ^{:key i}
         [workspace-comp nil it])]

      [:div
       {:class ["flex" "flex-1"]}
       [active-workspace selected-workspaces]]]]))
