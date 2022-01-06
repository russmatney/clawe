(ns doctor.ui.views.workspaces
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [
             [systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [clawe.workspaces :as clawe.workspaces]
             ]
       :cljs [
              [wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-workspaces []
  (->>
    (clawe.workspaces/all-workspaces)
    (filter :awesome/tag)))

#?(:clj
   (defsys *workspaces-stream*
     :start (s/stream)
     :stop (s/close! *workspaces-stream*)))

#?(:clj
   (comment
     (sys/start! `*workspaces-stream*)

     ))

(defstream workspaces-stream [] *workspaces-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-workspaces []
     (let [workspaces  (plasma.uix/state [])
           handle-resp (fn [new-wsps]
                         (swap! workspaces
                                (fn [wsps]
                                  (->>
                                    (concat (or wsps []) new-wsps)
                                    (w/distinct-by :workspace/title)
                                    (sort-by :awesome/index)))))]

       (with-rpc [] (get-workspaces) handle-resp)
       (with-stream [] (workspaces-stream) handle-resp)

       {:items @workspaces})))

#?(:cljs
   (defn workspace-comp
     ([wsp] (workspace-comp nil wsp))
     ([_opts wsp]
      (let [{:keys [workspace/title
                    git/repo
                    workspace/directory
                    workspace/color
                    workspace/title-hiccup
                    awesome/index
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
           [:div (or repo directory)])]))))

#?(:cljs
   (defn widget []
     (let [{:keys [items]} (use-workspaces)]
       [:div
        {:class ["p-4"]}
        [:h1
         {:class ["font-nes" "text-2xl" "text-white"
                  "pb-2"]}
         (str "Workspaces (" (count items) ")")]

        [:div
         {:class ["flex" "flex-row" "flex-wrap"
                  "justify-between"
                  ]}
         (for [[i it] (->> items (map-indexed vector))]
           ^{:key i}
           [workspace-comp nil it])]]
       )))
