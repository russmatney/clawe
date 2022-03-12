(ns ralphie.lock
  (:require
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]

   [ralphie.sh :as sh]))

;; i3lock-slick --filter blur:sigma=2

(defcom lock-screen
  "Run i3lock-slick"
  (do
    (notify/notify "doctor-cmd called")
    (sh/zsh "/home/russ/.cargo/bin/i3lock-slick --filter blur:sigma=2")))
