(ns clawe.i3
  (:require
   [clojure.string :as string]
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.i3 :as r.i3]))

(defn wsp->i3-wsp-name [wsp]
  (str (:workspace/index wsp) ": " (:workspace/title wsp)))

(defn i3-wsp->workspace-title [wsp]
  (string/replace (:i3/name wsp) #"^.*: ?" ""))

(defn ->wsp [wsp]
  (let [title (i3-wsp->workspace-title wsp)]
    (assoc wsp
           :workspace/title title
           :workspace/index (:i3/num wsp)
           :workspace/focused (:i3/focused wsp))))

(defn ->client [client]
  (assoc client
         :client/id (:i3/id client)
         :client/app-name (-> client :i3/window_properties :i3/class string/lower-case)
         :client/app-names (-> client :i3/window_properties ((juxt :i3/class :i3/instance)) (->> (map string/lower-case)))
         :client/window-title (-> client :i3/window_properties :i3/title string/lower-case)
         :client/focused (-> client :i3/focused)))

(defn attach-clients [wsp]
  (let [clients (r.i3/wsp->clients wsp)]
    (assoc wsp :workspace/clients (->> clients (map ->client)))))

(comment
  (->>
    (r.i3/workspaces)
    (map ->wsp)
    (map attach-clients)
    ))

(defrecord I3 []
  ClaweWM

  ;; workspaces

  (-current-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (some->>
      [(r.i3/current-workspace {:include-clients true})]
      (map ->wsp)
      (map attach-clients)))

  (-active-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (->>
      (r.i3/workspaces)
      (map ->wsp)
      (map attach-clients)))

  (-create-workspace [_this _opts workspace-title]
    (r.i3/create-workspace workspace-title))

  (-focus-workspace [_this _opts wsp]
    (r.i3/focus-workspace (or (:i3/num wsp) (:workspace/index wsp))))

  (-fetch-workspace [_this _opts workspace-title]
    (some-> workspace-title r.i3/workspace-for-name ->wsp attach-clients))

  (-swap-workspaces-by-index [_this a b]
    ;; input is indexes? or wsps?
    #_(r.i3/swap-workspaces a b))

  (-drag-workspace [_this dir])

  (-delete-workspace [_this workspace]
    ;; switch to this workspace, move it's contents elsewhere (scratchpad?)
    ;; then 'kill' it
    )

  ;; clients

  (-close-client [_this _opts c]
    (r.i3/close-client c))

  (-active-clients [_this _opts]
    (->>
      (r.i3/all-clients)
      (map ->client)))

  (-bury-client [_this _opts client]
    (r.i3/bury-client client))

  (-bury-clients [_this _opts clients]
    (r.i3/bury-clients clients))

  (-focus-client [_this _opts client]
    (r.i3/focus-client client))

  (-move-client-to-workspace [this _opts c wsp]
    (r.i3/move-client-to-workspace c wsp)))

(comment

  (def wsps
    (clawe.wm.protocol/-active-workspaces
      (I3.) nil))

  (clawe.wm.protocol/-swap-workspaces-by-index
    (I3.) (first wsps) (second wsps))

  (def clients
    (clawe.wm.protocol/-active-clients (I3.) nil))

  (nth clients 4)
  (clawe.wm.protocol/-focus-client
    (I3.) nil (nth clients 4))

  (nth clients 4)
  (clawe.wm.protocol/-move-client-to-workspace
    (I3.) nil (nth clients 4)
    (first (clawe.wm.protocol/-current-workspaces
             (I3.) nil)))

  )
