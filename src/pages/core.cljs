(ns pages.core
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [hiccup-icons.octicons :as octicons]
   ;; [keybind.core :as key]

   [components.icons :as components.icons]))


(defn menu [menu-opts]
  (when menu-opts
    (let [current-page-name (->
                              #_{:clj-kondo/ignore [:unresolved-var]}
                              router/*match* uix/context :data :name)]
      [:div
       {:class
        ["flex" "flex-col" "p-6"
         "text-city-pink-100"
         "text-xxl"
         "font-nes"]}
       (for [[i {:keys [page-name label]}]
             (->> menu-opts (map-indexed vector))]
         [:a {:key  i
              :class
              (concat
                ["hover:text-city-pink-500"]
                (cond
                  (#{current-page-name} page-name)
                  ["text-city-pink-400" "text-bold"]))
              :href (router/href page-name)} label])])))

(def page-error-boundary
  "Not sure if this is working yet....
  seems to not work for a simple `(throw \"yo\")` in a component body."
  (uix/create-error-boundary
    {:error->state (fn [e] {:error e})
     :handle-catch (fn [e info]
                     (println "error boundary handle-catch")
                     (println "Error!" e info))}
    (fn [state [child]]
      (let [{:keys [error]} @state]
        (if error
          [:div
           {:class ["bg-city-blue-900"
                    "min-h-screen"
                    "w-full"]}
           [:span "i'm the ghost of an error boundary"]
           [:pre error]]
          child)))))

(defn page
  "Accepts a main component and wraps it in page helpers with a menu."
  ;; TODO reduce this crazy arity nonsense
  ([main] [page [] main {}])
  ([main opts] [page [] main opts])
  ([menu-opts main page-opts]
   (let [_params           (router/use-route-parameters)
         current-page-name (->
                             #_{:clj-kondo/ignore [:unresolved-var]}
                             router/*match* uix/context :data :name)
         *menu-clicked?    (uix/state false)]
     [:div
      {:class ["bg-city-blue-900"
               "min-h-screen"
               "w-full"]}
      [page-error-boundary
       [:div {:class ["flex" "flex-row"
                      "bg-city-brown-600"
                      "shadow"
                      "shadow-city-brown-900"]}
        [:div
         {:class ["font-nes"
                  "pt-3" "pl-3"
                  "text-city-pink-200"
                  "capitalize"]}
         current-page-name]

        [:div
         {:class ["ml-auto"]}
         [:div
          {:class    ["p-3" "text-city-pink-100"
                      "cursor-pointer"
                      "hover:text-city-pink-400"]
           :on-click #(swap! *menu-clicked? not)}
          [components.icons/icon-comp
           {:text "Menu"
            :icon
            (if @*menu-clicked?
              octicons/chevron-left
              octicons/list-unordered)}]]]]]

      [:div
       {:class ["flex" "flex-row"]}
       (when main [main page-opts])
       (when @*menu-clicked?
         [:div
          {:class ["ml-auto"
                   "bg-city-blue-800"
                   "shadow-lg"
                   "shadow-city-pink-800"]}
          [menu menu-opts]])]])))
