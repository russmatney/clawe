(ns components.screenshot
  (:require
   [tick.core :as t]
   [uix.core.alpha :as uix]
   ["@headlessui/react" :as Headless]
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

(defn img [{:keys [file/web-asset-path] :as _screenshot}]
  [:img {:src web-asset-path}])

(defn screenshot-dialog
  [{:keys [open? on-close]}
   {:keys [name file/full-path
           file/web-asset-path]
    :as   _item}]
  [:> Headless/Dialog
   {:class ["relative"
            "z-50"]
    :open  open? :on-close on-close}

   [:div
    {:class       ["fixed"
                   "inset-0"
                   "bg-city-black-700"
                   "bg-opacity-60"]
     :aria-hidden "true"}]

   [:div {:class ["fixed inset-0 flex items-center justify-center"
                  "m-24"
                  "p-12"
                  "bg-city-black-200"]}

    [:> Headless/Dialog.Panel
     [:> Headless/Dialog.Title name]
     [:> Headless/Dialog.Description full-path]

     (when web-asset-path
       [:img {:src      web-asset-path
              :on-click #(on-close)
              :class    ["cursor-pointer"]}])]]])


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
        [:img {:src      web-asset-path
               :on-click #(reset! dialog-open? true)
               :class    ["cursor-pointer"]}])

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
        [:img {:src      web-asset-path
               :on-click #(reset! dialog-open? true)
               :class    ["cursor-pointer"]}])

      [screenshot-dialog
       {:open?    @dialog-open?
        :on-close (fn [_] (reset! dialog-open? false))}
       item]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster [opts screenshots]
  [:div
   {:class ["flex" "flex-row" "flex-wrap"]}

   (for [[i event] (->> screenshots
                        (sort-by :event/timestamp t/<)
                        (map-indexed vector))]
     [:div
      {:key   i
       :class ["m-2"]}

      (when (:file/web-asset-path event)
        [floating/popover
         {:click       true :hover true
          :anchor-comp [:div
                        {:class ["border-city-blue-400"
                                 "border-opacity-40"
                                 "border"
                                 "w-36"]}
                        [components.screenshot/thumbnail opts event]]
          :popover-comp
          [:div
           {:class ["w-2/3"
                    "shadow"
                    "shadow-city-blue-800"
                    "border"
                    "border-city-blue-800"]}
           [components.screenshot/img event]]}])])])
