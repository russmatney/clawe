(ns doctor.ui.topbar
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[doctor.api.topbar :as d.topbar]
             [ralphie.awesome :as awm]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-topbar-metadata [] (d.topbar/build-topbar-metadata))
(defstream topbar-metadata-stream [] d.topbar/*topbar-metadata-stream*)

#?(:cljs
   (defn use-topbar-metadata []
     (let [topbar-metadata (plasma.uix/state [])
           handle-resp     #(reset! topbar-metadata %)]

       (with-rpc [] (get-topbar-metadata) handle-resp)
       (with-stream [] (topbar-metadata-stream) handle-resp)

       @topbar-metadata)))

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
