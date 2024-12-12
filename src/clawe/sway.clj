(ns clawe.sway
  (:require
   [clojure.string :as string]
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.sway :as r.sway]))

(defn wsp->title [wsp]
  (-> wsp :sway/name
      (string/replace  #"^.*: ?" "")
      (string/trim)))

(defn ->wsp [wsp]
  (let [title (wsp->title wsp)]
    (assoc wsp
           :workspace/title title
           :workspace/index (:sway/num wsp)
           :workspace/focused (:sway/focused wsp))))

(defn ->client [client]
  (let [names (->>
                [(some-> client :sway/app_id)
                 (some-> client :sway/window_properties :sway/class)
                 (some-> client :sway/window_properties :sway/instance)]
                (remove nil?)
                (map string/lower-case))]
    (assoc client
           :client/id (:sway/id client)
           :client/app-name (some-> names first)
           :client/app-names names ;; TODO dedupe
           :client/window-title
           (:sway/name client
                       (some-> client :sway/window_properties :sway/title string/lower-case))
           :client/focused (:sway/focused client))))

(defn attach-clients [wsp]
  (let [clients (r.sway/wsp->clients wsp)]
    (assoc wsp :workspace/clients (->> clients (map ->client)))))

(defrecord Sway []
  ClaweWM

  ;; workspaces
  (-current-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (some->>
      [(r.sway/current-workspace)]
      (remove nil?)
      (map ->wsp)
      (map attach-clients)))

  (-active-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (->>
      (r.sway/workspaces)
      (map ->wsp)
      (map attach-clients)))

  (-create-workspace [_this _opts workspace-title]
    (r.sway/create-workspace {:name workspace-title}))

  (-focus-workspace [_this _opts wsp]
    (r.sway/focus-workspace {:num (or (:sway/num wsp) (:workspace/index wsp))}))

  (-fetch-workspace [_this _opts workspace-title]
    (some-> {:name workspace-title} r.sway/get-workspace ->wsp attach-clients))

  (-swap-workspaces-by-index [_this a b]
    (r.sway/swap-workspaces-by-index a b))

  (-drag-workspace [_this dir]
    (let [i   (case dir :dir/up 1 :dir/down -1)
          wsp (r.sway/current-workspace)]
      (r.sway/swap-workspaces-by-index (:sway/num wsp) (+ (:sway/num wsp) i))))

  (-delete-workspace [_this _workspace]
    ;; switch to this workspace, move it's contents elsewhere (scratchpad?)
    ;; then 'kill' it
    )

  ;; clients

  (-active-clients [_this _opts]
    (->>
      (r.sway/clients)
      (map ->client)))

  (-focus-client [_this _opts client]
    (r.sway/focus-client client))

  (-bury-client [_this _opts client]
    (r.sway/bury-client client))

  (-bury-clients [_this _opts clients]
    (r.sway/bury-clients clients))

  (-close-client [_this _opts c]
    (r.sway/close-client c))

  (-hide-scratchpad [_this _opts c]
    (r.sway/hide-scratchpad c))

  (-move-client-to-workspace [_this _opts c wsp]
    (when (and c wsp)
      (r.sway/move-client-to-workspace c wsp))))

(comment

  (def wsps
    (clawe.wm.protocol/-active-workspaces
      (Sway.) nil))

  (clawe.wm.protocol/-swap-workspaces-by-index
    (Sway.) (first wsps) (second wsps))

  (def clients
    (clawe.wm.protocol/-active-clients (Sway.) nil))

  (nth clients 4)

  )
