(ns components.chess
  (:require
   [clojure.string :as string]
   [components.floating :as floating]
   [components.debug]
   [tick.core :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; thumbnail
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn player-and-rating [{:keys [player rating-diff]}]
  [:div
   {:class ["flex" "flex-row"
            "gap-x-1"]}
   [:span
    {:class ["font-nes"
             (when (#{"russmatney"} player)
               "text-city-pink-300")]}
    player]

   (when (#{"russmatney"} player)
     [:span
      {:class [(cond
                 (> rating-diff 0) "text-city-green-400"
                 (< rating-diff 0) "text-city-red-500"
                 :else             "text-city-gray-500")]}
      (when (> rating-diff 0)
        "+")
      rating-diff])])

(defn thumbnail [opts game]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf
          ]} game]
    [:div
     {:class ["p-2"]}

     [:div
      {:class ["flex" "flex-row"
               "items-center"
               "gap-x-3"
               "font-mono"]}
      [:span
       {:class ["capitalize"]}
       perf]

      [player-and-rating {:player white-player :rating-diff white-rating-diff}]
      [:span "vs"]
      [player-and-rating {:player black-player :rating-diff black-rating-diff}]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; detail popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn detail-popover [opts game]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf opening-name
          ]} game]
    [:div
     {:class ["bg-yo-blue-500" "p-3"
              "text-city-blue-200"
              "flex" "flex-col"
              ]}

     [:div
      {:class ["flex" "flex-row"
               "items-center"
               "gap-x-3"
               "font-mono"]}
      [:span
       {:class ["capitalize"]}
       perf]

      [player-and-rating {:player white-player :rating-diff white-rating-diff}]
      [:span "vs"]
      [player-and-rating {:player black-player :rating-diff black-rating-diff}]]

     [:div
      {:class ["text-xl"]}
      opening-name]

     [components.debug/raw-metadata game]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster [opts games]
  (when (seq games)
    [:div
     {:class ["p-2"]}

     (for [game games]
       [:div
        {:key   (:lichess.game/id game)
         :class []}

        [floating/popover
         {:hover true
          :click true
          :anchor-comp
          [:div
           {:class ["cursor-pointer"]}
           [thumbnail opts game]]
          :popover-comp
          [detail-popover opts game]
          }]])]))
