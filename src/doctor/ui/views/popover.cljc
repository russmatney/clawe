(ns doctor.ui.views.popover
  (:require
   #?@(:cljs [[doctor.ui.components.icons :as icons]
              [doctor.ui.components.debug :as debug]
              [doctor.ui.views.wallpapers :as wp]
              [doctor.ui.views.todos :as td]
              [doctor.ui.views.screenshots :as sc]
              [uix.core.alpha :as uix]
              ])))

(defn is-bar-app? [client]
  (and
    (-> client :awesome.client/name #{"clover/doctor-dock"
                                      "clover/doctor-topbar"})
    (-> client :awesome.client/focused not)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Detail window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
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
          (->> client (sort-by first))]]))))

#?(:cljs
   (defn active-workspace [{:keys [active-workspaces hovered-workspace
                                   hovered-client]}
                           metadata]
     [:div
      (when (or (seq active-workspaces) hovered-workspace)
        (for [wsp (if hovered-workspace [hovered-workspace] active-workspaces)]
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
              [:span.text-xl.font-nes title]

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
                 [client-metadata client]))])))

      (when hovered-client
        [:div
         [:div.text-xl "Hovered Client"]
         [client-metadata {:initial-show? true} hovered-client]])

      [debug/raw-metadata {:label "Raw Topbar Metadata"}
       (->> metadata (sort-by first))]]))

#?(:cljs
   (defn tabs [opts metadata]
     (let [current (uix/state :tab/workspaces)]
       [:div
        [:div "Select tab"
         (for [t [:tab/workspaces
                  :tab/wallpapers
                  :tab/todos
                  :tab/screenshots]]
           ^{:key t}
           [:div.text-xl
            {:class    ["cursor-pointer"
                        "hover:text-city-red-400"
                        (when (#{t} @current)
                          "text-city-green-400")
                        ]
             :on-click #(reset! current t)}
            t])]

        (case @current
          :tab/workspaces  [active-workspace opts metadata]
          :tab/wallpapers  [wp/widget]
          :tab/todos       [td/widget]
          :tab/screenshots [sc/widget])])))

#?(:cljs
   (defn detail-window [{:keys [push-below] :as opts} metadata]
     [:div
      {:class          ["m-6" "ml-auto" "p-6"
                        "bg-yo-blue-500"
                        "bg-opacity-80"
                        "border-city-blue-400"
                        "rounded"
                        "w-2/3"
                        "text-white"
                        "overflow-y-auto"
                        "h-5/6" ;; scroll requires parent to have a height
                        ]
       :on-mouse-leave push-below}

      [tabs opts metadata]]))


#?(:cljs
   (defn popup [_opts _metadata]
     [:div
      [:div "Some popover"]]))
