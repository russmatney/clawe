(ns doctor.ui.views.workspaces
  (:require
   [clojure.string :as string]
   [doctor.ui.workspaces :as workspaces]))

(defn dir [s]
  (-> s (string/replace #"/home/russ" "~")))

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
       (str title " (" index ")")]

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
  (let [{:keys [workspaces]} (workspaces/use-workspaces)]
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
        [workspace-comp nil it])]]
    ))
