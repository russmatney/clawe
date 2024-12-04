(ns doctor.ui.hooks.plasma
  #?(:cljs (:require-macros [doctor.ui.hooks.plasma]))
  (:require
   [uix.core :as uix]
   #?@(:cljs [[promesa.core :as p]
              [plasma.client.stream :as s]])))

(defmacro with-stream
  "Macro to consume a stream"
  ([deps stream on-item]
   `(with-stream ~deps ~stream nil ~on-item))
  ([deps stream on-init on-item]
   `(let [init?#   (uix/use-state false)
          on-init# ~on-init
          on-item# ~on-item]
      (uix/use-effect
        (fn []
          (let [s# ~stream]
            (when on-init#
              (plasma.client.stream/on-initialized s# on-init#))
            (plasma.client.stream/consume-via!
              s# #(do (on-item# %) true))
            #(plasma.client.stream/close! s#)))
        ~deps))))

(defmacro with-rpc
  "Macro helper for making an RPC call"
  ([deps rpc on-success]
   `(with-rpc ~deps ~rpc ~on-success nil))
  ([deps rpc on-success on-fail]
   (let [cljs? (:ns &env)]
     (if-not cljs?
       `nil
       `(let [on-success# ~on-success
              on-fail#    ~on-fail]
          (uix/use-effect
            (fn []
              (let [rpc# ~rpc]
                (when rpc#
                  (cond-> rpc#
                    on-success# (promesa.core/then on-success#)
                    on-fail#    (promesa.core/catch on-fail#)))))
            ~deps))))))
