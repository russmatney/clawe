(ns components.actions
  (:require
   [components.icons :as icons]
   [components.floating :as floating]
   [components.colors :as colors]
   [uix.core.alpha :as uix]
   [hiccup-icons.fa :as fa]))

(defn actions-list [opts-or-axs]
  (let [actions (:actions opts-or-axs opts-or-axs)]
    (when actions
      (let [fallback-n (:n opts-or-axs 3)
            n          (uix/state fallback-n)
            show-all   (fn [] (reset! n (count actions)))
            collapse   (fn [] (reset! n fallback-n))
            actions    (->>
                         actions
                         (sort-by (fn [x] (:action/priority x 0)) >)
                         (take @n)
                         (into [])
                         ((fn [axs]
                            (conj axs
                                  (cond
                                    (> (dec (count actions)) @n)
                                    {:action/label    "show all"
                                     :action/on-click show-all
                                     :action/icon     fa/chevron-right-solid}

                                    (= @n (count actions))
                                    {:action/label    "show less"
                                     :action/on-click collapse
                                     :action/icon     fa/chevron-left-solid}

                                    :else nil)))))]
        [:div
         {:class ["inline-flex" "flex-wrap"]}
         (for [[i ax] (->> actions
                           (remove nil?)
                           (map-indexed vector))]
           ^{:key i}
           [icons/action-icon-button
            {:class (colors/color-wheel-classes {:i i :type :line})}
            ax])]))))

(defn actions-popup [opts]
  [floating/popover
   {:hover  true :click true
    :offset 0
    :anchor-comp
    (:comp opts
           [:div (:label opts "Actions")])
    :popover-comp
    [:div
     {:class ["bg-city-blue-400"]}
     [actions-list opts]]}])
