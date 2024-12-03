(ns doctor.ui.hooks.plasma
  ;; #?(:cljs (:require-macros [plasma.uix]))
  (:require
   ;; [plasma.uix.hot-reload :as hot-reload]
   ;; [uix.hooks.alpha :as hooks]
   ;; [uix.core.alpha :as uix]
   #?@(:cljs [[promesa.core :as p]
              [plasma.client.stream :as s]])))

;; (defmacro state
;;   "Hot reload aware state for "
;;   [val]
;;   (if-not (hot-reload/enabled? &env)
;;     `(hooks/state ~val)
;;     (let [hot-reload-id (hot-reload/get-identifier! &env &form)]
;;       `(let [val# (if-let [known-state# (get @hot-reload/cache ~hot-reload-id)]
;;                     @known-state#
;;                     ~val)
;;              res# (hooks/state val#)]
;;          (swap! hot-reload/cache assoc ~hot-reload-id res#)
;;          res#))))

;; (defmacro use-rpc
;;   "Macro helper for RPC calls using Plasma in UIX"
;;   [rpc-call & [deps]]
;;   (if-not (:ns &env)
;;     `{:data ~rpc-call :loading? false}
;;     `(let [closed?# (uix/ref false)
;;            state#   (state {:loading? false})]
;;        (hooks/effect! (fn []
;;                         (reset! state!# {:loading? true})
;;                         (some-> ~rpc-call
;;                                 (p/chain #(when-not @closed?# (reset! state!# {:loading? false :data %})))
;;                                 (p/catch #(when-not @closed?# (reset! state!# {:loading? false :error %}))))
;;                         (fn [] (reset! closed?# true)))
;;                       ~deps)
;;        state#)))

;; (defmacro with-stream
;;   "Macro to consume a stream"
;;   ([deps stream on-item]
;;    `(with-stream ~deps ~stream nil ~on-item))
;;   ([deps stream on-init on-item]
;;    `(let [init?#   (uix/state false)
;;           on-init# ~on-init
;;           on-item# ~on-item]
;;       (uix/with-effect ~deps
;;         (let [s# ~stream]
;;           (when on-init#
;;             (plasma.client.stream/on-initialized s# on-init#))
;;           (plasma.client.stream/consume-via!
;;             s# #(do (on-item# %) true))
;;           #(plasma.client.stream/close! s#))))))


;; (defmacro with-rpc
;;   "Macro helper for making an RPC call"
;;   ([deps rpc on-success]
;;    `(with-rpc ~deps ~rpc ~on-success nil))
;;   ([deps rpc on-success on-fail]
;;    (let [cljs? (:ns &env)]
;;      (if-not cljs?
;;        `nil
;;        `(let [on-success# ~on-success
;;               on-fail#    ~on-fail]
;;           (uix/with-effect ~deps
;;             (let [rpc# ~rpc]
;;               (when rpc#
;;                 (cond-> rpc#
;;                   on-success# (promesa.core/then on-success#)
;;                   on-fail#    (promesa.core/catch on-fail#))))))))))
