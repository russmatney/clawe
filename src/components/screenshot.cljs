(ns components.screenshot
  (:require
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
    :as   item}]
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

(defn hover-popup
  ([item] (hover-popup {} item))
  ([opts item]
   [:div
    [img item]]))

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
