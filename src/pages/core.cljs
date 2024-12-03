(ns pages.core
  (:require
   [hiccup-icons.octicons :as octicons]
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [defui $]]

   [components.icons :as components.icons]
   ))

(defui route-def->icon [{:keys [icon label]}]
  (let [icon-opts {:class ["px-1 py-2 pr-2"]
                   :text  label}]
    ($ components.icons/icon-comp
       (cond icon  (assoc icon-opts :icon icon)
             :else (assoc icon-opts :icon ($ octicons/alert))))))

(defui menu [{:keys [expanded? route-defs] :as opts}]
  (when (seq route-defs)
    ($ :div
       {:class ["flex" "flex-col" "p-3"]}
       (for [[i {:keys [route] :as route-def}]
             (->> route-defs
                  (remove :comp-only)
                  (map-indexed vector))]

         ($ :a {:key   i
                :class (concat
                         ["flex" "flex-row"
                          (when-not expanded? "justify-center")
                          "items-center"
                          "text-city-pink-100"
                          "text-xl"
                          "font-nes"
                          "hover:text-city-pink-500"]
                         (cond (#{(:route opts)} route)
                               ["text-city-pink-400" "text-bold"
                                "whitespace-nowrap"]))
                :href  (rfe/href route)
                }
            ($ route-def->icon route-def)

            (when expanded?
              ($ :span
                 {:class ["pl-2"]}
                 (:label route-def))))))))

(defui expanding-menu [opts]
  (let [[expanded? set-expanded]        (uix/use-state false)
        [expand-timer set-expand-timer] (uix/use-state nil)]
    ($ :div
       {:class
        (concat ["flex" "flex-col"
                 "transition-all ease-in-out"
                 "overflow-hidden"
                 "duration-300" "bg-city-blue-900"

                 "shadow"
                 "shadow-city-blue-400"]
                (if expanded? ["w-96"] ["w-16"]))
        :on-mouse-enter (fn [_] (set-expand-timer
                                  (js/setTimeout
                                    (fn []
                                      (set-expanded true)
                                      (js/clearTimeout expand-timer))
                                    150)))
        :on-mouse-leave (fn [_]
                          (js/clearTimeout expand-timer)
                          (set-expanded false))}
       ($ :div {:class ["fixed"]}
          ($ menu {:route-defs (:route-defs opts) :expanded? expanded?})))))

(def page-error-boundary
  (uix.core/create-class
    {:displayName              "error-boundary"
     :getInitialState          (fn [] #js {:error nil})
     :getDerivedStateFromError (fn [error] #js {:error error})
     :componentDidCatch        (fn [error _error-info]
                                 (this-as this
                                   (let [props (.. this -props -argv)]
                                     (when-let [on-error (:on-error props)]
                                       (on-error error)))))
     :render                   (fn []
                                 (this-as this
                                   (if (.. this -state -error)
                                     ($ :div
                                        {:class ["bg-city-blue-900"
                                                 "text-slate-200"
                                                 "min-h-screen"
                                                 "w-full"]}
                                        ($ :span "i'm the ghost of an error boundary")
                                        ($ :br)
                                        ($ :pre {:class ["whitespace-pre-wrap"]}
                                           (str (.. this -state -error))))
                                     (.. this -props -children))))}))

(defui page
  "Accepts a main component and wraps it in page helpers with a menu."
  [{:keys [main hide-header route-defs route] :as page-opts}]
  ($ page-error-boundary
     ($ :div {:class ["min-h-screen"
                      "flex" "flex-row"
                      "bg-city-blue-800"]}
        ($ :div {:class ["flex flex-col" "w-full"]}
           (when-not hide-header
             ($ :div
                {:class ["bg-city-blue-gray-600"
                         "shadow"
                         "shadow-city-brown-900"
                         "font-nes"
                         "p-3"
                         "text-city-pink-200"
                         "capitalize"
                         "flex flex-row" "items-center"]}
                (or (:label page-opts) route)))

           (when main ($ main page-opts)))

        ($ expanding-menu {:route-defs route-defs}))))
