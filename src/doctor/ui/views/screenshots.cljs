(ns doctor.ui.views.screenshots
  (:require
   [doctor.ui.screenshots :as screenshots]
   [uix.core.alpha :as uix]

   ["@headlessui/react" :as Headless]

   ))

(defn ->actions
  "Note these actions are tied to the component's dialog atom"
  [{:keys [dialog-open?]} item]
  (let [{:keys []} item]
    (->>
      [{:action/label    "js/alert"
        :action/on-click #(js/alert item)}
       {:action/label    "open dialog"
        :action/on-click (fn [_] (reset! dialog-open? true))}]
      (remove nil?))))


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
       [:img {:src web-asset-path}])]]])


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
        [:img {:src web-asset-path}])

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

(defn widget []
  (let [{:keys [items]} (screenshots/use-screenshots)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              ]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [screenshot-comp nil it])]))
