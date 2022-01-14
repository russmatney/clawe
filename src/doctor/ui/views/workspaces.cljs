(ns doctor.ui.views.workspaces
  (:require
   [clojure.string :as string]
   [doctor.ui.workspaces :as workspaces]
   [doctor.ui.components.icons :as icons]
   [doctor.ui.components.debug :as debug]

   [doctor.ui.topbar :as topbar]))

(defn dir [s]
  (-> s (string/replace #"/home/russ" "~")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry this up across views
(defn is-bar-app? [client]
  (and
    (-> client :awesome.client/name #{"tauri/doctor-topbar" "tauri/doctor-popup"})
    (-> client :awesome.client/focused not)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->actions [w]
  [{:action/label    "Update workspace display name"
    :action/on-click #(-> w
                          (assoc :workspace/display-name %)
                          (workspaces/update-workspace))}
   ]
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Detail window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-metadata
  ([client] [client-metadata nil client])
  ([opts client]
   (let [{:keys [awesome.client/name
                 awesome.client/class
                 awesome.client/instance]} client]
     [:div
      {:class ["flex" "flex-col" "mb-6"]}

      [:div.mb-4
       {:class ["flex" "flex-row"]}
       [icons/icon-comp
        (assoc (icons/client->icon client nil)
               :class ["w-8" "mr-4"])]

       [:span.text-xl
        (str name " | " class " | " instance)]]

      [debug/raw-metadata
       (merge {:label "Raw Client Metadata"} opts)
       (->> client (sort-by first))]])))

(defn active-workspace [active-workspaces]
  [:div
   {:class ["bg-yo-blue-500" "p-6" "text-white"]}
   (when (seq active-workspaces)
     (for [wsp active-workspaces]
       (let [{:keys [workspace/directory
                     git/repo
                     git/needs-push?
                     git/dirty?
                     git/needs-pull?
                     workspace/title
                     awesome.tag/clients]} wsp

             dir     (or directory repo)
             clients (->> clients (remove is-bar-app?))]

         ^{:key title}
         [:div
          {:class ["text-left"]}
          [:div
           {:class ["flex flex-row justify-between items-center"]}
           [:span.text-xl.font-nes (workspaces/workspace-name wsp)]

           [:span.ml-auto
            (str
              (when needs-push? (str "#needs-push"))
              (when needs-pull? (str "#needs-pull"))
              (when dirty? (str "#dirty")))]]

          [:div
           {:class ["mb-4" "font-mono"]}
           dir]

          [debug/raw-metadata
           {:label "Raw Workspace Metadata"}
           (->> wsp (sort-by first))]

          (when (seq clients)
            (for [client clients]
              ^{:key (:awesome.client/window client)}
              [client-metadata client]))])))])

;; TODO restore
;; (defn topbar-metadata []
;;   (let [metadata (topbar/use-topbar)]
;;     [debug/raw-metadata {:label "Raw Topbar Metadata"}
;;      (->> metadata (sort-by first))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-comp
  ([wsp] (workspace-comp nil wsp))
  ([_opts wsp]
   (let [{:keys [workspace/title
                 git/repo
                 workspace/directory
                 workspace/color
                 workspace/title-hiccup
                 awesome.tag/index
                 workspace/scratchpad
                 awesome/clients
                 ]} wsp]
     [:div
      {:class ["m-1"
               "p-4"
               "border"
               "border-city-blue-600"
               "bg-yo-blue-700"
               "text-white"]}
      [:div
       (when color {:style {:color color}})
       (str (workspaces/workspace-name wsp) " (" index ")")]

      [:div
       (when scratchpad
         (str "#scratchpad"))

       (when repo
         (str "#repo"))]

      (when (seq clients)
        [:div
         (for [c (->> clients)]
           ^{:key (:window c)}
           [:div (str "- '" (:name c) "'")])])

      (when title-hiccup
        [:div title-hiccup])

      (when (or repo directory)
        [:div (dir (or repo directory))])])))

(defn widget []
  (let [{:keys [workspaces active-workspaces]} (workspaces/use-workspaces)]
    [:div
     {:class ["p-4"]}
     [:h1
      {:class ["font-nes" "text-2xl" "text-white"
               "pb-2"]}
      (str "Workspaces (" (count workspaces) ")")]

     [:div
      {:class ["flex" "flex-row" "flex-wrap"
               "justify-between"
               ]}
      (for [[i it] (->> workspaces (map-indexed vector))]
        ^{:key i}
        [workspace-comp nil it])]

     [active-workspace active-workspaces]]))
