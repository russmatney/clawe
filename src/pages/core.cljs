(ns pages.core
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [hiccup-icons.octicons :as octicons]
   [components.icons :as components.icons]))


(defn route-def->icon [{:keys [page-name label]}]
  [components.icons/icon-comp
   (case page-name
     nil {:icon octicons/alert}
     {:text label})])


(defn menu [{:keys [expanded? route-defs]}]
  (when (seq route-defs)
    (let [current-page-name (->
                              #_{:clj-kondo/ignore [:unresolved-var]}
                              router/*match* uix/context :data :name)]
      [:div
       {:class
        ["flex" "flex-col" "p-6"
         "text-city-pink-100"
         "text-xxl"
         "font-nes"]}
       (for [[i {:keys [page-name] :as route-def}]
             (->> route-defs
                  (remove :comp-only)
                  (map-indexed vector))]
         [:a {:key   i
              :class (concat
                       ["hover:text-city-pink-500"]
                       (cond (#{current-page-name} page-name)
                             ["text-city-pink-400" "text-bold"]))
              :href  (router/href page-name)}
          (route-def->icon (assoc route-def :expanded? expanded?))])])))

(defn expanding-menu [route-defs]
  (let [expanded? (uix/state true)]
    [:div
     {:class
      (concat ["flex" "flex-col"
               "ml-auto"
               "transition-all ease-in-out"
               "overflow-hidden"
               "duration-300"]
              (if @expanded? [] ["w-12"]))}
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
      {:class ["bg-city-blue-800"
               "shadow-lg"
               "shadow-city-pink-800"]}
      [menu {:route-defs route-defs
             :expanded?  @expanded?}]]]))

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
  ([main] [page main nil])
  ([main {:keys [hide-header route-defs] :as page-opts}]
   (let [params            (router/use-route-parameters)
         page-opts         (assoc page-opts :params params)
         current-page-name (-> #_{:clj-kondo/ignore [:unresolved-var]}
                               router/*match* uix/context :data :name)]
     [page-error-boundary
      [:div {:class ["min-h-screen" "bg-city-blue-900"
                     "w-full" "flex" "flex-row"]}
       [:div
        {:class ["flex flex-col" "w-full"]}

        (when-not hide-header
          [:div
           {:class ["bg-city-brown-600"
                    "shadow"
                    "shadow-city-brown-900"
                    "font-nes"
                    "pt-3" "pl-3"
                    "text-city-pink-200"
                    "capitalize"]}
           current-page-name])

        [:div
         {:class ["bg-city-blue-900" "w-full"]}

         (when main [main page-opts])]]

       [expanding-menu route-defs]]])))
