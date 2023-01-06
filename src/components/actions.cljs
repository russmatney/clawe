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
    {:action/keys [label icon comp on-click tooltip disabled]
     :as          action}]
   (let [ax-class           (:action/class action)
         text-color-class   (->> (concat ax-class class ["text-city-blue-700"])
                                 (filter is-text-color-class?)
                                 first)
         border-color-class (->> (concat ax-class class ["border-city-blue-700"])
                                 (filter is-border-color-class?)
                                 first)
         class              (->> class
                                 (remove is-text-color-class?)
                                 (remove is-border-color-class?))
         ax-class           (->> ax-class
                                 (remove is-text-color-class?)
                                 (remove is-border-color-class?))]
     [:div
      {:class
       (concat
         ["px-2" "py-1"
          "rounded" "border"
          "flex"
          "justify-center"
          "items-center"
          "tooltip"
          "relative"]
         (when icon ["w-9" "h-9"])
         ax-class
         class
         (if disabled
           ["border-slate-600" "text-slate-600"]
           ["cursor-pointer"
            text-color-class
            border-color-class
            "hover:text-city-blue-300"
            "hover:border-city-blue-300"]))
       :on-click (fn [_] (when (and on-click (not disabled)) (on-click)))}
      [:div (or comp icon label)]
      [:div
       {:class ["tooltip"
                "tooltip-text"
                "bottom-10"
                ;; TODO get wise about where to put this tooltip
                ;; this prevents width overflow if the actions list is near the right edge
                ;; but otherwise it looks weird
                "-left-20"
                "whitespace-nowrap"]}
       (or tooltip label)]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; action list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn actions-list [opts-or-axs]
  (let [actions (:actions opts-or-axs opts-or-axs)]
    (when actions
      (let [fallback-page-size (:n opts-or-axs 3)
            page-size          (uix/state fallback-page-size)
            showing-all        (uix/state false)
            show-all           (fn []
                                 (reset! page-size (count actions))
                                 (reset! showing-all true))
            collapse           (fn []
                                 (reset! page-size fallback-page-size)
                                 (reset! showing-all false))
            actions            (->>
                                 (cond->> actions
                                   (:hide-disabled opts-or-axs)
                                   (remove :action/disabled))
                                 (sort-by
                                   (fn [x]
                                     (if (:action/disabled x)
                                       -100 ;; disabled come last
                                       (:action/priority x 0)))
                                   >)
                                 (take @page-size)
                                 (into [])
                                 ((fn [axs]
                                    (conj axs
                                          (cond
                                            (and (> (count actions) @page-size)
                                                 (not @showing-all))
                                            {:action/label    "show all"
                                             :action/on-click show-all
                                             :action/icon     fa/chevron-right-solid}

                                            (and
                                              (= @page-size (count actions))
                                              @showing-all)
                                            {:action/label    "show less"
                                             :action/on-click collapse
                                             :action/icon     fa/chevron-left-solid}

                                            :else nil)))))]
        [:div
         {:class ["inline-flex"
                  (when (or (not (:nowrap opts-or-axs)) @showing-all)
                    "flex-wrap")]}
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
     {:class ["bg-slate-800"]}
     [actions-list opts]]}])
