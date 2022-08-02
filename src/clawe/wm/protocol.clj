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
  (-ensure-workspace [this opts workspace-title])
  (-focus-workspace [this opts workspace] "Should create the workspace if missing.")
  (-current-clients [this opts] "Return all clients in the current active workspace.")
  (-all-clients [this opts] "Returns all running clients.")
  (-focus-client [this opts client])
  (-close-client [this opts client])
  (-move-client-to-workspace [this opts client workspace]))
