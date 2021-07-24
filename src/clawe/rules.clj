(ns clawe.rules
  (:require
   [clawe.workspaces :as workspaces]
   [defthing.defcom :refer [defcom]]
   ;; [ralphie.notify :as notify]
   [clojure.edn :as edn]))


(defn enforce-awesome-rules
  "Reads awesome rules from the workspace, updates awesomeWM's global rules obj."
  []
  (let [wsp-awm-rules (->> (workspaces/all-workspaces)
                           (keep :awesome/rules))]
    ;; TODO some awm-fnl function for updating the rules
    wsp-awm-rules))

(defcom apply-rules-to-client
  "Fired from various awesomeWM signals, asking clawe to apply rules
to the passed client obj."
  (fn [_ & arguments]
    ;; (notify/notify "apply-rules" arguments)
    (let [
          ;; arg (some-> arguments first first str edn/read-string)
          ]
      ;; TODO figure out which workspace this client is relevant for
      ;; check if it already belongs to that workpace
      ;; move it if necessary
      ;; might also just call :rules/apply if there's a scratchpad-classes hit
      ;; longer term - maybe drop awesome rules completely and apply them in here
      ;; need to be sure all clients are captured here,
      ;; and that tag destinations are deterministic
      ;; (println "client-tagged signal" arg)
      ;; (notify/notify "client-tagged signal" arg)
      )))
