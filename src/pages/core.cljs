(ns pages.core
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [hiccup-icons.octicons :as octicons]
   ;; [keybind.core :as key]

   [pages.events]
   [components.floating :as floating]
   [components.icons]))

(defn default-page [opts]
  [:div
   [:p.text-city-pink-100.p-4
    "No app selected, defaulting..."]
   [pages.events/page]])

(defn menu [menu-opts]
  (when menu-opts
    (let [current-page-name (-> router/*match* uix/context :data :name)]
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

(defn page
  "Accepts a main component and wraps it in page helpers with a menu."
  ([] [page default-page {}])
  ([main] [page [] main {}])
  ([main opts] [page [] main opts])
  ([menu-opts main page-opts]
   (let [params            (router/use-route-parameters)
         main              (or main default-page)
         current-page-name (-> router/*match* uix/context :data :name)]
     [:div
      {:class ["bg-city-blue-900"
               "min-h-screen"
               "w-full"]}
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

       [floating/popover
        {:click             true
         :offset            10
         :anchor-comp-props {:class ["ml-auto"]}
         :anchor-comp
         [:div
          {:class ["p-3" "text-city-pink-100"
                   "cursor-pointer"
                   "hover:text-city-pink-400"]}
          [components.icons/icon-comp
           {:text "Menu"
            :icon octicons/list-unordered}]]
         :popover-comp
         [:div
          {:class ["bg-city-blue-800"
                   "shadow-lg"
                   "shadow-city-pink-800"
                   ]}
          [menu menu-opts]]}]]

      (when main [main page-opts])])))
