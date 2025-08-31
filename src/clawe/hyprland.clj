(ns clawe.hyprland
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.hyprland :as r.hypr]))

(comment
  (println "are we there yet")

  (r.hypr/list-workspaces))

(defn ->wsp [wsp]
  (-> wsp
      (merge {:workspace/title (:hyprland/name wsp)
              :workspace/index (:hyprland/id wsp)
              }))
  )

(defn ->client [wsp]
  (-> wsp
      (merge {
              :client/id           (:hyprland/pid wsp)
              :client/app-name     (:hyprland/class wsp)
              :client/window-title (:hyprland/title wsp)
              }))
  )

(defn attach-clients [wsp]
  wsp)

(defrecord Hyprland []
  ClaweWM

  ;; workspaces
  (-current-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (some->>
      [(r.hypr/get-active-workspace)]
      (remove nil?)
      (map ->wsp)
      (map attach-clients)))

  (-active-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (->>
      (r.hypr/list-workspaces)
      (map ->wsp)
      (map attach-clients)))

  ;; (-create-workspace [_this _opts workspace-title]
  ;;   (r.sway/create-workspace {:name workspace-title}))

  ;; (-focus-workspace [_this _opts wsp]
  ;;   (r.sway/focus-workspace {:num (or (:sway/num wsp) (:workspace/index wsp))}))

  ;; (-fetch-workspace [_this _opts workspace-title]
  ;;   (some-> {:name workspace-title} r.sway/get-workspace ->wsp attach-clients))

  ;; (-swap-workspaces-by-index [_this a b]
  ;;   (r.sway/swap-workspaces-by-index a b))

  ;; (-drag-workspace [_this dir]
  ;;   (let [i   (case dir :dir/up 1 :dir/down -1)
  ;;         wsp (r.sway/current-workspace)]
  ;;     (r.sway/swap-workspaces-by-index (:sway/num wsp) (+ (:sway/num wsp) i))))

  ;; (-delete-workspace [_this _workspace]
  ;;   ;; switch to this workspace, move it's contents elsewhere (scratchpad?)
  ;;   ;; then 'kill' it
  ;;   )

  ;; clients

  (-active-clients [_this _opts]
    (->>
      (r.hypr/list-clients)
      (map ->client)))

  ;; (-focus-client [_this _opts client]
  ;;   (r.sway/focus-client client))

  ;; (-bury-client [_this _opts client]
  ;;   (r.sway/bury-client client))

  ;; (-bury-clients [_this _opts clients]
  ;;   (r.sway/bury-clients clients))

  ;; (-close-client [_this _opts c]
  ;;   (r.sway/close-client c))

  ;; (-hide-scratchpad [_this _opts c]
  ;;   (r.sway/hide-scratchpad c))

  #_(-move-client-to-workspace [_this _opts c wsp]
      (when (and c wsp)
        (r.sway/move-client-to-workspace c wsp))))

(comment
  ;; (wm.protocol/-active-workspaces Hyprland nil)
  )
