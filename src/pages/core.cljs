(ns pages.core
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [hiccup-icons.octicons :as octicons]
   [components.icons :as components.icons]))


(defn menu-opt->icon [{:keys [page-name label]}]
  [components.icons/icon-comp
   (case page-name
     nil {:icon octicons/alert}
     {:text label})])


(defn menu [{:keys [expanded? menu-opts]}]
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
       (for [[i {:keys [page-name] :as menu-opt}]
             (->> menu-opts (map-indexed vector))]
         [:a {:key   i
              :class (concat
                       ["hover:text-city-pink-500"]
                       (cond (#{current-page-name} page-name)
                             ["text-city-pink-400" "text-bold"]))
              :href  (router/href page-name)}
          (menu-opt->icon (assoc menu-opt :expanded? expanded?))])])))

(defn expanding-menu [menu-opts]
  (let [expanded? (uix/state true)]
    [:div
     {:class
      (concat ["flex" "flex-col"
               "ml-auto"
               "transition-all ease-in-out"
               "duration-300"]
              (if @expanded?
                ["translate-x-0"
                 "w-64"]
                ["translate-x-4/5"
                 "w-12"]))}
     [:div
      {:class    ["p-3" "text-city-pink-100"
                  "cursor-pointer"
                  "hover:text-city-pink-400"]
       :on-click #(swap! expanded? not)}
      [components.icons/icon-comp
       {:text "Menu"
        :icon
        (if @expanded?
          octicons/chevron-right
          octicons/list-unordered)}]]
     [:div
      {:class ["ml-auto"
               "flex"
               "bg-city-blue-800"
               "shadow-lg"
               "shadow-city-pink-800"]}
      [menu {:menu-opts menu-opts
             :expanded? @expanded?}]]]))

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
                             router/*match* uix/context :data :name)]
     [:div
      [page-error-boundary
       [:div {:class ["min-h-screen" "bg-city-blue-900" "w-full"
                      "flex" "flex-row"]}
        [:div
         {:class ["flex flex-col" "w-full"]}

         [:div
          {:class ["bg-city-brown-600"
                   "shadow"
                   "shadow-city-brown-900"
                   "font-nes"
                   "pt-3" "pl-3"
                   "text-city-pink-200"
                   "capitalize"]}
          current-page-name]

         [:div
          {:class ["bg-city-blue-900" "w-full"]}

          (when main [main page-opts])]]

        [expanding-menu menu-opts]]]])))
