(ns hooks.topbar
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
