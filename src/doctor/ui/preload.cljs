(ns doctor.ui.preload
  (:require
   ;; [taoensso.telemere :as t]
   [uix.dev :as uix.dev]))

;; initializes fast-refresh runtime
(uix.dev/init-fast-refresh!)

;; called by shadow-cljs after every reload
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load refresh []
  (println :info "refresh!")
  ;; performs components refresh
  (uix.dev/refresh!))
