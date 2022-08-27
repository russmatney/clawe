(ns clawe.yabai
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.yabai :as yabai]))

;; TODO refactor in malli transforms?
(defn window->clawe-client
  [{:yabai.window/keys [has-focus app title] :as window}]
  (-> window
      (assoc :client/focused has-focus)
      (assoc :client/window-title title)
      (assoc :client/app-name app)))

(defn space->clawe-workspace
  [{:yabai.space/keys [index label has-focus] :as space}]
  (-> space
      (assoc :workspace/index index)
      (assoc :workspace/title (if (seq label)
                                label
                                (str "wsp-" index)))
      (assoc :workspace/focused has-focus)))

(defn clients-for-space [{:yabai.space/keys [windows]} clients]
  (->> windows
       (map (fn [window-id]
              (->> clients
                   (filter (comp #{window-id} :yabai.window/id))
                   first)))
       (remove nil?)))

(defrecord Yabai []
  ClaweWM
  (-current-workspaces [this opts]
    (let [clients (when (or (:include-clients opts)
                            (:prefetched-clients opts))
                    (or (:prefetched-clients opts)
                        (clawe.wm.protocol/-active-clients this nil)))
          spc     (yabai/query-current-space)]
      (-> spc space->clawe-workspace
          (assoc :workspace/clients (or (clients-for-space spc clients) []))
          vector)))

  (-active-workspaces [this opts]
    (let
        [clients (when (or (:include-clients opts)
                           (:prefetched-clients opts))
                   (or (:prefetched-clients opts)
                       (clawe.wm.protocol/-active-clients this nil)))]
      (->>
        (yabai/query-spaces)
        (map space->clawe-workspace)
        (map (fn [wsp]
               (assoc wsp :workspace/clients
                      (or (clients-for-space wsp clients) [])))))))

  (-create-workspace [_this _opts workspace-title]
    (yabai/ensure-labeled-space
      {:space-label         workspace-title
       :overwrite-unlabeled true}))

  (-focus-workspace [_this _opts wsp-or-label]
    (let [workspace-title (if (string? wsp-or-label)
                            wsp-or-label (:workspace/title wsp-or-label))]
      (yabai/ensure-labeled-space {:space-label         workspace-title
                                   :overwrite-unlabeled true})
      (yabai/focus-space wsp-or-label)))

  (-fetch-workspace [this opts workspace-title]
    (let
        [clients (when (or (:include-clients opts)
                           (:prefetched-clients opts))
                   (or (:prefetched-clients opts)
                       (clawe.wm.protocol/-active-clients this nil)))
         wsp
         (some->>
           ;; TODO optimize
           (yabai/query-spaces)
           (filter (comp #{workspace-title} :yabai.space/label))
           (map space->clawe-workspace)
           first
           space->clawe-workspace)]
      (when wsp
        (-> wsp
            (assoc :workspace/clients (or (clients-for-space wsp clients) []))))))

  (-swap-workspaces-by-index [_this a b]
    (yabai/swap-spaces-by-index a b))

  (-drag-workspace [_this dir]
    (yabai/drag-workspace dir))

  (-delete-workspace [_this workspace]
    (yabai/destroy-space {:space-label (:workspace/title workspace)}))

  ;; clients

  (-active-clients [_this _opts]
    (->>
      (yabai/query-windows)
      (map window->clawe-client)))

  (-focus-client [_this opts c]
    (when (:float-and-center opts) (yabai/float-and-center-window c))
    (yabai/focus-window c))

  (-close-client [_this _opts c]
    (yabai/close-window c))

  (-move-client-to-workspace [this _opts c wsp]
    (let [workspace-title (if (string? wsp) wsp (:workspace/title wsp))
          workspace-index (when (map? wsp) (:workspace/index wsp))]
      ;; ensure this workspace exists
      (clawe.wm.protocol/-create-workspace this nil workspace-title)
      (yabai/move-window-to-space c (or workspace-index workspace-title)))))

(comment
  (clawe.wm.protocol/-current-workspaces (Yabai.) nil)
  (clawe.wm.protocol/-fetch-workspace
    (Yabai.) nil "doesntexist")

  (clawe.wm.protocol/-current-workspaces
    (Yabai.) {:include-clients true}))
