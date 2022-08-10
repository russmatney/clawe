(ns ralphie.screenshot
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.rofi :as rofi]
   [ralphie.notify :refer [notify]]
   [babashka.process :refer [$ check]]))

(defn select-region
  "Depends on ~/.local/bin/screenshot-region"
  []
  (-> ($ screenshot-region)
      check)
  (notify "Selected region screenshot captured"))

;; TODO optionally disable transparency before shot
(defn full-screen
  "Depends on ~/.local/bin/screenshot
  TODO make sure this script reports an error when it fails
  and consider moving the script logic into clj
  "
  []
  (-> ($ screenshot)
      check)
  (notify "Fullscreen screenshot captured"))

(defcom take-screenshot
  {:doctor/depends-on ["screenshot" "screenshot-region"]}
  (fn [_cmd & args]
    (let [arg (some-> args first)]
      (prn "Taking screenshot with arg" arg)
      (cond
        (= arg "full")
        (full-screen)

        (= arg "region")
        (select-region)

        :else
        (rofi/rofi {:msg "Full screen or select region?"}
                   [{:label     "full screen"
                     :on-select (fn [_arg] (full-screen))}
                    {:label     "select region"
                     :on-select (fn [_arg] (select-region))}])))))
