(ns clawe.i3
  (:require
   [clojure.string :as string]
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.i3 :as r.i3]))

(defrecord I3 []
  ClaweWM

  ;; workspaces

  (-current-workspaces [_this opts])

  (-active-workspaces [_this opts])

  (-create-workspace [_this _opts workspace-title])

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
