(ns ralphie.picom
  (:require
   [defthing.defcom :refer [defcom]]
   [babashka.process :refer [$ check]]))

(defcom inc-opacity-handler
  "increase opacity"
  (->  ($ picom-trans -s +5) check))

(defcom dec-opacity-handler
  "decrease opacity"
  (->  ($ picom-trans -s -5) check))
