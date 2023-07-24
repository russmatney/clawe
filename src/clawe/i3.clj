(ns clawe.i3
  (:require
   [clojure.string :as string]
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.i3 :as r.i3]))

(defn ->wsp [i3-wsp]
  (let [title (string/replace (:i3/name i3-wsp) #"^.*: ?" "")]
    (assoc i3-wsp
           :workspace/title title
           :workspace/index (:i3/num i3-wsp)
           :workspace/focused (:i3/focused i3-wsp))))

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
    )

  (-focus-workspace [_this _opts workspace])

  (-fetch-workspace [_this opts workspace-title])

  (-swap-workspaces-by-index [_this a b])

  (-drag-workspace [_this dir])

  (-delete-workspace [_this workspace])

  ;; clients

  (-close-client [_this _opts c])

  (-active-clients [_this _opts])

  (-bury-client [_this _opts client])

  (-bury-clients [_this _opts clients])

  (-focus-client [_this opts client])

  (-move-client-to-workspace [this _opts c wsp]))
