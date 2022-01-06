(ns ralphie.monitor
  (:require
   [defthing.defcom :refer [defcom]]
   [ralphie.rofi :as rofi]
   [babashka.process :refer [$ check]]))

;; TODO refactor rest of script into here, show dimensions in desc
(defcom set-monitor-resolution
  (fn  [_cmd & arguments]
    (let [label (some-> arguments first)
          label (or label (rofi/rofi {:msg       "Set monitor-resolution to:"
                                      :on-select :label}
                                     [{:label "laptop"}
                                      {:label "laptop-hr"}
                                      {:label "home"}
                                      {:label "home-monitor-only"}
                                      {:label "algo"}]))]
      (when label
        (-> ($ set-monitor-config ~label)
            check)))))
