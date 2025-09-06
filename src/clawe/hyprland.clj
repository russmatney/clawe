(ns clawe.hyprland
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.hyprland :as r.hypr]
   [clojure.string :as string]
   ))

;; fetch/data helpers

(defn ->wsp [wsp]
  (-> wsp
      (merge {:workspace/title      (:hypr/name wsp)
              :workspace/index      (:hypr/id wsp)
              :workspace/scratchpad (string/starts-with? (:hypr/name wsp) "special:")})))

(defn ->client [wsp]
  (-> wsp
      (merge {:client/id           (:hypr/pid wsp)
              :client/app-name     (:hypr/class wsp)
              :client/window-title (:hypr/title wsp)})))

(defn attach-clients [wsp]
  wsp)

;; workspace funcs

(defn current-workspace [_opts]
  (-> (r.hypr/get-active-workspace) ->wsp attach-clients))

(defn list-workspaces [{:keys [exclude-scratchpads?]}]
  (cond->> (r.hypr/list-workspaces)
    true                 (map ->wsp)
    exclude-scratchpads? (remove :workspace/scratchpad)
    true                 (map attach-clients)))

(defn list-scratchpads []
  (->>
    (list-workspaces nil)
    (filter :workspace/scratchpad)))

(comment
  (list-workspaces {:exclude-scratchpads? true})
  (list-scratchpads)
  (r.hypr/create-workspace {:name "dino"}))

;; client funcs

(defn active-clients [opts]
  (->> (r.hypr/list-clients)
       (map ->client)))

(comment
  (active-clients nil))

(defrecord Hyprland []
  ClaweWM

  ;; workspaces
  (-current-workspaces [_this {:keys [_prefetched-clients] :as opts}]
    [(current-workspace opts)])

  (-active-workspaces [_this {:keys [_prefetched-clients] :as opts}]
    (list-workspaces opts))

  (-create-workspace [_this _opts workspace-title]
    (r.hypr/create-workspace {:name workspace-title}))

  (-focus-workspace [_this _opts wsp]
    (r.hypr/focus-workspace {:id (or (:hypr/id wsp) (:workspace/index wsp))}))

  (-fetch-workspace [_this _opts workspace-title]
    (some-> {:name workspace-title} r.hypr/get-workspace ->wsp attach-clients))

  (-swap-workspaces-by-index [_this a b])
  (-drag-workspace [_this dir])
  (-delete-workspace [_this _workspace])

  ;; clients

  (-active-clients [_this opts]
    (active-clients opts))

  (-focus-client [_this _opts client]
    (r.hypr/focus-client client))

  (-bury-client [_this _opts client])
  (-bury-clients [_this _opts clients])

  (-close-client [_this _opts c]
    (r.hypr/close-client c))

  (-hide-scratchpad [_this _opts c])

  (-move-client-to-workspace [_this _opts c wsp]
    (when (and c wsp)
      (r.hypr/move-client-to-workspace c wsp))))
