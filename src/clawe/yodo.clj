(ns clawe.yodo
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.emacs :as r.emacs]
   [clawe.awesome :as awm]
   [clojure.string :as string]))

(defcom update-org-clock-cmd
  {:name          "update-org-clock"
   :one-line-desc "Pulls the last org-clock from emacs"
   :description   ["Updates the org-clock widget."]
   :handler
   (fn [_ _]
     (let [clock-string
           (-> (r.emacs/emacs-cli "(russ/current-clock-string)")
               string/trim-newline
               read-string)]
       (awm/awm-cli
         (awm/awm-fn "update_org_clock_widget" (str clock-string)))))})
