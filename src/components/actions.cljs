(ns components.actions
  (:require
   [components.floating :as floating]
   [components.colors :as colors]
   [uix.core.alpha :as uix]
   [hiccup-icons.fa :as fa]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; icon button
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-text-color-class? [s]
  (re-seq #"text-.*-\d\d\d" s))

(defn is-border-color-class? [s]
  (re-seq #"border-.*-\d\d\d" s))

(comment
  (is-text-color-class? "text-city-blue-700")
  (is-text-color-class? "text-xl")
  (is-border-color-class? "border-city-blue-700"))

(defn action-icon-button
  ([action] (action-icon-button nil action))
  ([{:keys [class]}
    {:action/keys [label icon on-click tooltip disabled]
     :as          action}]
   (let [ax-class     (:action/class action)
         text-class   (->> (concat ax-class class ["text-city-blue-700"])
                           (filter is-text-color-class?)
                           first)
         border-class (->> (concat ax-class class ["border-city-blue-700"])
                           (filter is-border-color-class?)
                           first)
         class        (->> class
                           (remove is-text-color-class?)
                           (remove is-border-color-class?))
         ax-class     (->> ax-class
                           (remove is-text-color-class?)
                           (remove is-border-color-class?))]
     [:div
      {:class
       (concat
         ["px-2" "py-1"
          "rounded" "border"
          "flex" "items-center"
          "tooltip"
          "relative"
          text-class
          border-class]
         ax-class
         class
         (if disabled
           ["border-slate-600" "text-slate-600"]
           ["cursor-pointer"
            "hover:text-city-blue-300"
            "hover:border-city-blue-300"]))
       :on-click (fn [_] (when (and on-click (not disabled)) (on-click)))}
      [:div (if icon icon label)]
      [:div.tooltip.tooltip-text.bottom-10.-left-3
       {:class ["whitespace-nowrap"]}
       (or tooltip label)]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; action list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
           [action-icon-button
            {:class (colors/color-wheel-classes {:i i :type :line})}
            ax])]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
