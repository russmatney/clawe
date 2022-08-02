(ns clawe.yabai
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.yabai :as yabai]))

(defn window->clawe-client
  [{:yabai.window/keys [has-focus app title] :as window}]
  (-> window
      (assoc :client/focused has-focus)
      (assoc :client/window-title title)
      (assoc :client/app-name app)))

(defn space->clawe-workspace
  [{:yabai.space/keys [index label] :as space}]
  (-> space
      (assoc :workspace/index index)
      (assoc :workspace/title label)))

(defn clients-for-space [{:yabai.space/keys [windows]} clients]
  (->> windows
       (map (fn [window-id]
              (->> clients
                   (filter (comp #{window-id} :yabai.window/id))
                   first)))))

(defrecord Yabai []
  ClaweWM
  (-current-workspaces [this opts]
    (let [clients (when (or (:include-clients opts)
                            (:prefetched-clients opts))
                    (or (:prefetched-clients opts)
                        (clawe.wm.protocol/-all-clients this nil)))
          spc     (yabai/query-current-space)]
      (-> spc space->clawe-workspace
          (assoc :workspace/clients (clients-for-space spc clients))
          vector)))

  (-active-workspaces [_this _opts]
    (->>
      (yabai/query-spaces)
      (map space->clawe-workspace)))

  (-ensure-workspace [_this _opts workspace-title]
    (yabai/ensure-labeled-space
      {:space-label         workspace-title
       :overwrite-unlabeled true}))

  (-focus-workspace [_this _opts workspace]
    (let [workspace-title (if (string? workspace)
                            workspace (:workspace/title workspace))]
      (yabai/ensure-labeled-space {:space-label         workspace-title
                                   :overwrite-unlabeled true})
      (yabai/focus-space {:space-label workspace-title})))

  (-fetch-workspace [_this _opts workspace-title]
    (->>
      ;; TODO optimize
      (yabai/query-spaces)
      (filter (comp #{workspace-title} :yabai.space/label))
      first
      space->clawe-workspace))

  (-swap-workspaces-by-index [_this a b]
    (yabai/swap-spaces-by-index a b))

  (-delete-workspace [_this workspace]
    (yabai/destroy-space {:space-label (:workspace/title workspace)}))

  ;; clients

  (-all-clients [_this _opts]
    (->>
      (yabai/query-windows)
      (map window->clawe-client)))

  (-focus-client [_this opts c]
    (when (:float-and-center opts) (yabai/float-and-center-window c))
    (yabai/focus-window c))

  (-close-client [_this _opts c]
    (yabai/close-window c))

  (-move-client-to-workspace [this opts c wsp]
    (let [workspace-title (if (string? wsp) wsp (:workspace/title wsp))
          workspace-index (when (map? wsp) (:workspace/index wsp))]
      (when (:ensure-workspace opts)
        (clawe.wm.protocol/-ensure-workspace this nil workspace-title))
      (yabai/move-window-to-space c (or workspace-index workspace-title)))))

(comment
  (clawe.wm.protocol/-current-workspaces
    (Yabai.) {:include-clients true}))
