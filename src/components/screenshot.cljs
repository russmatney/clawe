(ns components.screenshot
  (:require
   [tick.core :as t]
   [uix.core :as uix :refer [defui $]]

   [components.dialog :as dialog]
   [components.floating :as floating]))


(defn ->actions
  "NOTE these actions are tied to the component's dialog atom"
  [{:keys [dialog-open?]} item]
  (let [{:keys []} item]
    (->>
      [{:action/label    "js/alert"
        :action/on-click #(js/alert item)}
       {:action/label    "open dialog"
        :action/on-click (fn [_] (reset! dialog-open? true))}]
      (remove nil?))))

(defui img [{:keys [screenshot]}]
  (let [{:keys [file/web-asset-path]} screenshot

        ext (->> web-asset-path
                 ;; TODO do this on backend
                 reverse (take 3) reverse (apply str))]
    (case ext
      "mp4" ($ :video {:controls true}
               ($ :source {:src web-asset-path :type "video/mp4"}))
      ($ :img {:src web-asset-path}))))

(defui screenshot-dialog
  [{:keys [open? on-close screenshot] :as opts}]
  (let [{:keys [name file/full-path file/web-asset-path]} screenshot]
    ($ dialog/dialog
       {:open        open?
        :on-close    on-close
        :title       name
        :description full-path
        :content
        (when web-asset-path
          ($ :div
             {:class    ["cursor-pointer"]
              :on-click #(on-close)}
             ($ img opts)))})))


(defui screenshot-comp
  [{:keys [screenshot] :as opts}]
  (let [{:keys [file/web-asset-path]}  screenshot
        [_hovering? set-hovering]      (uix/use-state false)
        [dialog-open? set-dialog-open] (uix/use-state false)]
    ;; TODO if in focus, add keybinding for opening

    ($ :div
       {:class
        ["m-1" "p-4"
         "border" "border-city-blue-600"
         "bg-yo-blue-700"
         "text-white"]
        :on-mouse-enter #(set-hovering true)
        :on-mouse-leave #(set-hovering false)}
       (when web-asset-path
         ($ :div {:on-click #(set-dialog-open true)
                  :class    ["cursor-pointer"]}
            ($ img opts)))

       ($ screenshot-dialog
          (assoc opts
                 :open? dialog-open?
                 :on-close (fn [_] (set-dialog-open false))))

       ($ :div
          (for [ax (->actions {:dialog-open? dialog-open?} screenshot)]
            ($ :div
               {:key      (:action/label ax)
                :class    ["cursor-pointer"
                           "hover:text-yo-blue-300"]
                :on-click (:action/on-click ax)}
               (:action/label ax)))))))

(defui thumbnail
  [{:keys [screenshot] :as opts}]
  (let [{:keys [file/web-asset-path]}  screenshot
        [_hovering? set-hovering]      (uix/use-state false)
        [dialog-open? set-dialog-open] (uix/use-state false)]
    ($ :div
       {:on-mouse-enter #(set-hovering true)
        :on-mouse-leave #(set-hovering false)}
       (when web-asset-path
         ($ :div {:on-click #(set-dialog-open true)
                  :class    ["cursor-pointer"]}
            ($ img opts)))

       ($ screenshot-dialog
          {:open?    dialog-open?
           :on-close (fn [_] (set-dialog-open false))}
          screenshot))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui cluster-single [{:keys [screenshot] :as opts}]
  ($ :div
     {:class ["m-2"]}

     (when (:file/web-asset-path screenshot)
       ($ floating/popover
          {:click       true :hover true
           :anchor-comp ($ :div
                           {:class ["border-city-blue-400"
                                    "border-opacity-40"
                                    "border"
                                    "w-36"]}
                           ($ thumbnail opts))
           :popover-comp
           ($ :div
              {:class ["w-2/3"
                       "shadow"
                       "shadow-city-blue-800"
                       "border"
                       "border-city-blue-800"]}
              ($ img opts))}))))

(defui cluster [{:keys [screenshots] :as opts}]
  ($ :div
     {:class ["flex" "flex-row" "flex-wrap"]}

     (for [[i event] (->> screenshots
                          (sort-by :event/timestamp t/<)
                          (map-indexed vector))]
       ($ cluster-single (assoc opts
                                :screenshot event
                                :key i)))))
