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

  (-all-clients [_this _opts]
    (->>
      (yabai/query-windows)
      (map window->clawe-client))))

(comment
  (clawe.wm.protocol/-current-workspaces
    (Yabai.) {:include-clients true}))
