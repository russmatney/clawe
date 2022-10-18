(ns pages.workspaces
  (:require
   [clojure.string :as string]
   [hooks.workspaces :as hooks.workspaces]
   [components.icons :as icons]
   [components.debug :as debug]))

(defn dir [s]
  (-> s (string/replace #"/home/russ" "~")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry this up across views
(defn is-bar-app? [client]
  (and
    (-> client :awesome.client/name #{"tauri-doctor-topbar"})
    (-> client :awesome.client/focused not)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Detail window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-detail
  ([client] [client-detail nil client])
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
        (cond
          (#{(string/lower-case class)} (string/lower-case instance))
          (str class " | " name)

          :else
          (str class " | " instance " | " name))]]

      [debug/raw-metadata
       (merge {:label "Raw Client Metadata"} opts)
       (->> client (sort-by first))]])))

(defn active-workspace [active-workspaces]
  [:div
   {:class ["bg-yo-blue-500" "p-6" "text-white" "w-full"]}
   (when (seq active-workspaces)
     (for [wsp active-workspaces]
       (let [{:keys [workspace/directory
                     git/repo
                     git/needs-push?
                     git/dirty?
                     git/needs-pull?
                     workspace/title
                     awesome.tag/clients
                     tmux/session
                     ]} wsp

             dir     (or directory repo)
             clients (->> clients (remove is-bar-app?))]

         ^{:key title}
         [:div
          {:class ["text-left"]}
          [:div
           {:class ["flex flex-row justify-between items-center"]}
           [:span.text-xl.font-nes (:workspace/title wsp)]

           [:span.ml-auto
            (str
              (when needs-push? (str "#needs-push"))
              (when needs-pull? (str "#needs-pull"))
              (when dirty? (str "#dirty")))]]

          [:div
           {:class ["mb-4" "font-mono"]}
           dir]

          (when session
            [debug/raw-metadata
             {:label "Raw Tmux Session Metadata"}
             session])

          [debug/raw-metadata
           {:label "Raw Workspace Metadata"}
           wsp]

          [:div
           {:class ["flex flex-row justify-between items-center" "py-2"]}
           [:span.text-lg.font-nes (str (count clients) " client(s)")]]

          (when (seq clients)
            (for [client clients]
              ^{:key (:awesome.client/window client)}
              [client-detail client]))])))])

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
   (let [{:keys [git/repo
                 workspace/directory
                 workspace/color
                 workspace/title-hiccup
                 awesome.tag/index
                 awesome.tag/clients]} wsp]
     [:div
      {:class ["m-1"
               "p-4"
               "border"
               "border-city-blue-600"
               "bg-yo-blue-700"
               "text-white"
               "w-96"
               ]}
      [:div
       (when color {:style {:color color}})
       (str "(" index ") " (:workspace/title wsp))]

      [:div
       (when repo
         (str "#repo"))]

      (when (seq clients)
        [:ul
         {:class ["truncate"]}
         (for [c (->> clients)]
           ^{:key (:awesome.client/window c)}
           [:li
            (str (:awesome.client/class c) " | "
                 (:awesome.client/name c))])])

      (when title-hiccup
        [:div title-hiccup])

      (when (or repo directory)
        [:div (dir (or repo directory))])])))

(defn page [_opts]
  (let [{:keys [selected-workspaces active-workspaces]} (hooks.workspaces/use-workspaces)]
    [:div
     {:class ["p-4"]}
     [:h1
      {:class ["font-nes" "text-2xl" "text-white" "pb-2"]}
      (str "Workspaces (" (count active-workspaces) ")")]

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
