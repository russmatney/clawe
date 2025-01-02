(ns components.clients
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.icons :as icons]
   ))

(defui bar-icon
  [{:keys [color icon src
           fallback-text
           classes]}]
  ($ :div
     {:class (concat [color] classes)}
     (cond
       src   ($ :img {:class ["w-10"] :src src})
       icon  ($ icon)
       :else fallback-text)))

(defui client-icon-list
  "A list of icons. workspace is used for workspace-specific client icons (e.g. emacs)."
  [{:keys [workspace clients skip-client? small-icons?]}]
  (when (seq clients)
    ($ :div
       {:class ["flex" "flex-row" "flex-wrap"
                (when small-icons? "w-16")]}
       (for [[i c] (cond->> clients
                     skip-client? (remove skip-client?)
                     true         (sort-by :client/focused >)
                     true         (map-indexed vector))]
         (let [c-name                       (some->> c :client/window-title (take 15) (apply str))
               {:client/keys [focused]}     c
               {:keys [color] :as icon-def} (icons/client->icon c workspace)]
           ($ :div
              {:class [(if small-icons? "w-4" "w-8")
                       (when (and small-icons? focused) "mr-2")]
               :key   i}
              ($ bar-icon (-> icon-def
                              (assoc
                                :fallback-text c-name
                                :color color
                                :classes ["border-opacity-0"
                                          (cond
                                            focused "text-city-orange-400"
                                            color   color
                                            :else   "text-city-blue-400")])))))))))
