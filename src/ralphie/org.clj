(ns ralphie.org
  (:require
   [defthing.defcom :refer [defcom]]
   [babashka.process :refer [$ check]]))

(defcom rebuild-agenda-views
  (->
    ($ emacsclient -e "(org-batch-store-agenda-views)")
    check))
