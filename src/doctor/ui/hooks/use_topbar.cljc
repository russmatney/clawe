(ns doctor.ui.hooks.use-topbar
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.topbar]
             [ralphie.awesome :as awm]]
       :cljs [[uix.core :as uix]
              [doctor.ui.hooks.plasma :refer [with-stream with-rpc]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-topbar-metadata [] (api.topbar/build-topbar-metadata))
(defstream topbar-metadata-stream [] api.topbar/*topbar-metadata-stream*)

#?(:cljs
   (defn use-topbar-metadata []
     (let [[topbar-metadata set-metadata] (uix/use-state [])]

       (with-rpc [] (get-topbar-metadata) set-metadata)
       (with-stream [] (topbar-metadata-stream) set-metadata)

       topbar-metadata)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO probably drop this
(defhandler toggle-topbar-above [above?]
  (if above?
    ;; awm-fnl does not yet support passed arguments
    (awm/awm-fnl
      '(->
         (client.get)
         (lume.filter (fn [c] (= c.name "clover/doctor-topbar")))
         (lume.first)
         ((fn [c]
            (tset c :above true)
            (tset c :below false)
            (tset c :ontop true)))))
    (awm/awm-fnl
      '(do
         ;; bury everything else
         (-> (client.get)
             (lume.filter (fn [c] c.ontop))
             (lume.map (fn [c] (tset c :ontop false))))

         ;; keep topbar ontop
         (->
           (client.get)
           (lume.filter (fn [c] (= c.name "clover/doctor-topbar")))
           (lume.first)
           ((fn [c]
              (tset c :above false)
              (tset c :below true)
              (tset c :ontop false)))))))
  above?)

(defhandler set-background-mode [bg-mode]
  (api.topbar/set-background-mode bg-mode))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar widget and state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-topbar-state []
     (let [[hovered-client set-hovered-client]                 (uix/use-state nil)
           [hovered-workspace set-hovered-workspace]           (uix/use-state nil)
           [last-hovered-client set-last-hovered-client]       (uix/use-state nil)
           [last-hovered-workspace set-last-hovered-workspace] (uix/use-state nil)
           [topbar-above set-topbar-above]                     (uix/use-state true)
           toggle-above-below                                  (fn []
                                                                 (-> (toggle-topbar-above (not topbar-above))
                                                                     (.then (fn [v] (set-topbar-above v)))))
           ]

       {:hovered-client         hovered-client
        :hovered-workspace      hovered-workspace
        :last-hovered-workspace last-hovered-workspace
        :last-hovered-client    last-hovered-client
        :on-hover-workspace     (fn [w]
                                  (set-last-hovered-workspace w)
                                  (set-hovered-workspace w)
                                  ;; (pull-above)
                                  )
        :on-unhover-workspace   (fn [_] (set-hovered-workspace nil))
        :on-hover-client        (fn [c]
                                  (set-last-hovered-client c)
                                  (set-hovered-client c)
                                  ;; (pull-above)
                                  )
        :on-unhover-client      (fn [_] (set-hovered-client nil))
        :topbar-above           topbar-above
        :toggle-above-below     toggle-above-below
        })))
