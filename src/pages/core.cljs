(ns pages.core
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [hiccup-icons.octicons :as octicons]
   [components.icons :as components.icons]))


(defn route-def->icon [{:keys [icon label]}]
  (let [icon-opts {:class ["px-1 py-2 pr-2"]
                   :text  label}]
    [components.icons/icon-comp
     (cond icon  (assoc icon-opts :icon icon)
           :else (assoc icon-opts :icon octicons/alert))]))


(defn menu [{:keys [expanded? route-defs]}]
  (when (seq route-defs)
    (let [current-page-name
          (-> #_{:clj-kondo/ignore [:unresolved-var]}
              router/*match* uix/context :data :name)]
      [:div
       {:class ["flex" "flex-col" "p-3"]}
       (for [[i {:keys [page-name] :as route-def}]
             (->> route-defs
                  (remove :comp-only)
                  (map-indexed vector))]

         (let [icon-comp (route-def->icon route-def)]
           [:a {:key   i
                :class (concat
                         ["flex" "flex-row"
                          (when-not expanded? "justify-center")
                          "items-center"
                          "text-city-pink-100"
                          "text-xl"
                          "font-nes"
                          "hover:text-city-pink-500"]
                         (cond (#{current-page-name} page-name)
                               ["text-city-pink-400" "text-bold"
                                "whitespace-nowrap"]))
                :href  (router/href page-name)}
            (when icon-comp icon-comp)

            (when expanded?
              [:span
               {:class ["pl-2"]}
               (:label route-def)])]))])))

(defn expanding-menu [route-defs]
  (let [expanded?       (uix/state false)
        toggle-expanded (fn ([] (swap! expanded? not))
                          ([val] (reset! expanded? val)))
        expand-timer    (uix/state nil)]
    [:div
     {:class
      (concat ["flex" "flex-col"
               "transition-all ease-in-out"
               "overflow-hidden"
               "duration-300" "bg-city-blue-900"

               "shadow"
               "shadow-city-blue-400"]
              (if @expanded? ["w-96"] ["w-16"]))
      :on-mouse-enter #(do (reset! expand-timer
                                   (js/setTimeout
                                     (fn []
                                       (toggle-expanded true)
                                       (js/clearTimeout @expand-timer))
                                     150)))
      :on-mouse-leave #(do (js/clearTimeout @expand-timer)
                           (toggle-expanded false))}
     [:div {:class ["fixed"]}
      [menu {:route-defs      route-defs
             :expanded?       @expanded?
             :toggle-expanded toggle-expanded}]]]))

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
                    "text-slate-200"
                    "min-h-screen"
                    "w-full"]}
           [:span "i'm the ghost of an error boundary"]
           [:br]
           [:pre {:class ["whitespace-pre-wrap"]}
            error]]
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
      [:div {:class ["min-h-screen"
                     "flex" "flex-row"
                     "bg-city-blue-800"]}
       [:div {:class ["flex flex-col" "w-full"]}
        (when-not hide-header
          [:div
           {:class ["bg-city-blue-gray-600"
                    "shadow"
                    "shadow-city-brown-900"
                    "font-nes"
                    "p-3"
                    "text-city-pink-200"
                    "capitalize"
                    "flex flex-row" "items-center"]}
           (or (:label page-opts) current-page-name)])

        (when main [main page-opts])]

       [expanding-menu route-defs]]])))
