(ns components.screenshot
  (:require
   [tick.core :as t]
   [uix.core.alpha :as uix]
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

(defn img [{:keys [file/web-asset-path]}]
  (let [ext (->> web-asset-path
                 ;; TODO do this on backend
                 reverse (take 3) reverse (apply str))]
    (case ext
      "mp4" [:video {:controls true}
             [:source {:src web-asset-path :type "video/mp4"}]]
      [:img {:src web-asset-path}])))

(defn screenshot-dialog
  [{:keys [open? on-close]}
   {:keys [name file/full-path file/web-asset-path] :as item}]
  [dialog/dialog
   {:open        open?
    :on-close    on-close
    :title       name
    :description full-path
    :content
    (when web-asset-path
      [:div
       {:class    ["cursor-pointer"]
        :on-click #(on-close)}
       [img item]])}])


(defn screenshot-comp
  ([item] (screenshot-comp nil item))
  ([_opts item]
   (let [{:keys [file/web-asset-path]} item
         hovering?                     (uix/state false)
         dialog-open?                  (uix/state false)]
     ;; TODO if in focus, add keybinding for opening

     [:div
      {:class
       ["m-1" "p-4"
        "border" "border-city-blue-600"
        "bg-yo-blue-700"
        "text-white"]
       :on-mouse-enter #(do (reset! hovering? true))
       :on-mouse-leave #(do (reset! hovering? false))}
      (when web-asset-path
        [:div {:on-click #(reset! dialog-open? true)
               :class    ["cursor-pointer"]}
         [img item]])

      [screenshot-dialog
       {:open?    @dialog-open?
        :on-close (fn [_] (reset! dialog-open? false))}
       item]

      [:div
       (for [ax (->actions {:dialog-open?
                            dialog-open?} item)]
         ^{:key (:action/label ax)}
         [:div
          {:class    ["cursor-pointer"
                      "hover:text-yo-blue-300"]
           :on-click (:action/on-click ax)}
          (:action/label ax)])]])))

(defn thumbnail
  ([item] (screenshot-comp nil item))
  ([_opts item]
   (let [{:keys [file/web-asset-path]} item
         hovering?                     (uix/state false)
         dialog-open?                  (uix/state false)]
     [:div
      {:on-mouse-enter #(do (reset! hovering? true))
       :on-mouse-leave #(do (reset! hovering? false))}
      (when web-asset-path
        [:div {:on-click #(reset! dialog-open? true)
               :class    ["cursor-pointer"]}
         [img item]])

      [screenshot-dialog
       {:open?    @dialog-open?
        :on-close (fn [_] (reset! dialog-open? false))}
       item]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster-single [opts scr]
  [:div
   {:class ["m-2"]}

   (when (:file/web-asset-path scr)
     [floating/popover
      {:click       true :hover true
       :anchor-comp [:div
                     {:class ["border-city-blue-400"
                              "border-opacity-40"
                              "border"
                              "w-36"]}
                     [components.screenshot/thumbnail opts scr]]
       :popover-comp
       [:div
        {:class ["w-2/3"
                 "shadow"
                 "shadow-city-blue-800"
                 "border"
                 "border-city-blue-800"]}
        [components.screenshot/img scr]]}])])

(defn cluster [opts screenshots]
  [:div
   {:class ["flex" "flex-row" "flex-wrap"]}

   (for [[i event] (->> screenshots
                        (sort-by :event/timestamp t/<)
                        (map-indexed vector))]
     ^{:key i}
     [cluster-single opts event])])

(comment
  (cluster nil nil))
