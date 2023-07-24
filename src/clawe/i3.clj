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

(comment
  (->>
    (r.i3/workspaces-simple)
    (map ->wsp)))

(defrecord I3 []
  ClaweWM

  ;; workspaces

  (-current-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    ;; TODO support prefetching/including clients
    (some->>
      [(r.i3/current-workspace)]
      (map ->wsp)))

  (-active-workspaces [_this {:keys [_prefetched-clients] :as _opts}]
    (->>
      (r.i3/workspaces-simple)
      (map ->wsp)))

  (-create-workspace [_this _opts workspace-title]
    (r.i3/create-workspace workspace-title))

  (-focus-workspace [_this _opts wsp]
    (r.i3/visit-workspace (or (:i3/num wsp) (:workspace/index wsp))))

  (-fetch-workspace [_this _opts workspace-title]
    (-> workspace-title r.i3/workspace-for-name ->wsp))

  (-swap-workspaces-by-index [_this a b]
    ;; input is indexes? or wsps?
    #_(r.i3/swap-workspaces a b))

  (-drag-workspace [_this dir])

  (-delete-workspace [_this workspace]
    ;; switch to this workspace, move it's contents elsewhere (scratchpad?)
    ;; then 'kill' it
    )

  ;; clients

  (-close-client [_this _opts c])

  (-active-clients [_this _opts])

  (-bury-client [_this _opts client])

  (-bury-clients [_this _opts clients])

  (-focus-client [_this opts client])

  (-move-client-to-workspace [this _opts c wsp]))

(comment

  (def wsps
    (clawe.wm.protocol/-active-workspaces
      (I3.) nil))

  (clawe.wm.protocol/-swap-workspaces-by-index
    (I3.) (first wsps) (second wsps))
  )
