(ns clawe.wm.protocol)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ClaweWM
  "A protocol for typical window management functions."
  ;; TODO rename from 'current' to 'selected'
  (-current-workspaces
    [this opts] "Return the focused workspaces as a vector of maps.")
  (-active-workspaces [this opts] "Return all workspaces as a vector of maps.")
  (-create-workspace [this opts workspace-title]
    "Create a new workspace. No-ops if the workspace exists.")
  (-focus-workspace [this opts workspace]
    "Focus the passed workspace. Creates the workspace if missing.")
  (-fetch-workspace [this opts workspace-title])
  (-swap-workspaces-by-index [this index-a index-b])
  (-drag-workspace [this dir])
  (-delete-workspace [this workspace])

  (-active-clients [this opts] "Returns all running clients.")
  (-focus-client [this opts client])
  (-bury-all-clients [this opts] "Tile (unfloat) all clients")
  (-bury-client [this opts client] "Unfloats clients, pushing them into the base tiling layer")
  (-close-client [this opts client])
  (-move-client-to-workspace [this opts client workspace]))
