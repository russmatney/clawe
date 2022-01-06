(ns doctor.ui.views.screenshots
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [clawe.screenshots :as c.screenshots]]
       :cljs [[wing.core :as w]
              [uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn active-screenshots []
     (let [all (c.screenshots/all-screenshots)]
       (->> all (take 30) (into [])))))

(defhandler get-screenshots []
  (active-screenshots))

#?(:clj
   (defsys *screenshots-stream*
     :start (s/stream)
     :stop (s/close! *screenshots-stream*)))

(defstream screenshots-stream [] *screenshots-stream*)

#?(:clj
   (defn update-screenshots []
     (println "pushing to screenshots stream (updating screenshots)!")
     (s/put! *screenshots-stream* (active-screenshots))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-screenshots []
     (let [screenshots (plasma.uix/state [])
           handle-resp (fn [new-items]
                         (swap! screenshots
                                (fn [_]
                                  (->> new-items (w/distinct-by :file/full-path)))))]

       (with-rpc [] (get-screenshots) handle-resp)
       (with-stream [] (screenshots-stream) handle-resp)

       {:items @screenshots})))

#?(:cljs
   (defn ->actions [item]
     (let [{:keys []} item]
       (->>
         [{:action/label    "js/alert"
           :action/on-click #(js/alert item)}]
         (remove nil?)))))

#?(:cljs
   (defn screenshot-comp
     ([item] (screenshot-comp nil item))
     ([_opts item]
      (let [{:keys [;; name file/full-path
                    file/web-asset-path]} item
            hovering?                     (uix/state false)]

        [:div
         {:class
          ["m-1" "p-4"
           "border" "border-city-blue-600"
           "bg-yo-blue-700"
           "text-white"]
          :on-mouse-enter #(do (reset! hovering? true))
          :on-mouse-leave #(do (reset! hovering? false))}
         (when web-asset-path
           [:img {:src   web-asset-path
                  :class ["max-h-100"]}])

         [:div
          (for [ax (->actions item)]
            ^{:key (:action/label ax)}
            [:div
             {:class    ["cursor-pointer"
                         "hover:text-yo-blue-300"]
              :on-click (:action/on-click ax)}
             (:action/label ax)])]]))))

#?(:cljs
   (defn widget []
     (let [{:keys [items]} (use-screenshots)]
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
                 "min-h-screen"
                 "overflow-hidden"
                 "bg-yo-blue-700"
                 ]}
        (for [[i it] (->> items (map-indexed vector))]
          ^{:key i}
          [screenshot-comp nil it])])))
