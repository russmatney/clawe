(ns components.chess
  (:require
   [clojure.string :as string]
   [components.floating :as floating]
   [components.debug]
   [tick.core :as t]

   ["chessground" :refer [Chessground]]
   ["chess.js" :as Chess]
   [uix.core.alpha :as uix]

   [wing.core :as w]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; chessground wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn chessground [{:keys [shapes autoShapes] :as opts}]
  (let [opts   (clj->js (dissoc opts :shapes :autoShapes))
        cg-api (uix/state nil)
        ref    (uix/ref)]

    ;; set/destroy cg-api
    (uix/with-effect [ref]
      (when (and (not @cg-api) @ref)
        (reset! cg-api (Chessground. @ref opts)))
      #(when @cg-api (.destroy @cg-api)))

    ;; set config once api is set
    (uix/with-effect [@cg-api opts]
      (when @cg-api
        (.set @cg-api opts)
        ;; set shapes cannot be combined with :fen, so we call it after
        ;; https://github.com/lichess-org/chessground/issues/171
        (when shapes
          (.setShapes @cg-api (clj->js shapes)))
        (when autoShapes
          (.setAutoShapes @cg-api (clj->js autoShapes)))))

    [:div {:class ["w-full" "h-full"] :ref ref}]))

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
          perf analysis
          ]} game]
    [:div
     {:class ["py-2"]}

     [:div
      {:class ["flex" "flex-row"
               "items-end"
               "gap-x-3"
               "font-mono"]}
      [:span
       {:class ["capitalize"]}
       perf]

      [player-and-rating {:player white-player :rating-diff white-rating-diff}]
      [:span "vs"]
      [player-and-rating {:player black-player :rating-diff black-rating-diff}]

      (when (seq analysis)
        [:span
         {:class ["text-sm"]}
         "[Analysis available]"])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn display-game-state
  [{:keys [fen move best eval mate judgment move-number
           all-game-states
           i
           game]
    :as   _game-state}]
  (let [state-cursor                        (uix/state i)
        {:lichess.game/keys [white-player]} game]
    [:div
     {:class     ["flex" "flex-col" "overflow-y-scroll"]
      :on-scroll (fn [_]
                   (println "scroll detect")
                   )
      }
     [:div
      {:class ["flex" "flex-row"]}
      [:span {:class ["font-mono"]}
       (str move-number ". " (get move "san"))]
      (when eval
        [:span
         {:class ["ml-auto" "font-nes"]}
         (str (when (> eval 0) "+") (/ eval 100))])]

     (when mate
       [:span
        {:class ["ml-auto" "font-nes"]}
        (str "mate in " mate)])

     [:div
      {:class ["w-64" "h-64"]}
      ;; TODO scroll to move forward/back moves
      ;; TODO click to play out variation
      [chessground
       {:fen         fen
        :lastMove    #js [(get move "from") (get move "to")]
        ;; :viewOnly    true
        :coordinates false
        :orientation (if (#{"russmatney"} white-player)
                       "white" "black")
        :width       "220px"
        :height      "220px"
        :autoShapes  (->> [(when best
                             (let [[orig dest] (->> best (partition 2 2)
                                                    (map #(apply str %)))]
                               {:orig orig :dest dest :brush "blue"}))]
                          (remove nil?))}]]

     (when judgment [:span {:class ["font-mono"]}
                     ;; the name is sometimes repeated in the comment, so we
                     ;; remove it
                     (str (:name judgment) ".")
                     (->
                       (:comment judgment)
                       (string/replace (str (:name judgment) ".") ""))])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; detail popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn default-game-state-filter [states]
  (->>
    (concat
      (->> (range (dec (count states)))
           (filter (comp #{4} #(mod % 10)))
           (map #(nth states % nil)))
      [(last states)])
    (remove nil?)
    ;; could be removing valid dupes :shrug:
    (w/distinct-by identity)
    (take 4)))

(defn list-game-states
  ([game] [list-game-states {} game])
  ([{:keys [filter-game-states]
     :or   {filter-game-states default-game-state-filter}
     :as   opts}
    game]
   (let [{:lichess.game/keys [moves analysis]} game
         chessjs-inst                          (Chess/Chess.)

         all-game-states
         (->> (string/split moves #" ")
              (map-indexed
                (fn [i move]
                  (let [analysis (nth (or analysis []) i nil)
                        move     (.move chessjs-inst move)
                        f        (.fen chessjs-inst)]
                    (merge
                      {:i           i
                       :fen         f
                       :move        (js->clj move)
                       :move-number (+ 0.5 (/ (inc i) 2))}
                      analysis)))))]

     [:div
      {:class ["flex" "flex-row" "gap-2" "flex-wrap"]}
      (for [[i game-state]
            (->> all-game-states filter-game-states (map-indexed vector))]
        ^{:key i}
        [display-game-state
         (assoc game-state
                :all-game-states all-game-states
                :game game)])])))

(comment
  (let [[orig dest]
        (->> "e4e6" (partition 2 2)
             (map #(apply str %)))]
    [orig dest])
  )


(defn detail-popover [opts game]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf opening-name moves
          url]} game]
    [:div
     {:class ["bg-yo-blue-500" "p-3"
              "text-city-blue-200"
              "flex" "flex-col"]}

     [:div
      {:class ["flex" "flex-row"]}
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
       {:class ["ml-auto"]}

       [:div
        [:a
         {:href   url
          :target "_blank"
          :class  ["hover:city-pink-400"
                   "city-pink-200"
                   "cursor-pointer"]}
         "View on lichess"]]]]

     [:div
      {:class ["text-xl"]}
      opening-name]

     [:div
      {:class []}
      (str (/ (count (string/split moves #" ")) 2) " moves")]

     ;; highlights
     [list-game-states
      ;; {:filter-game-states (fn [game-states] game-states)}
      game]

     ;; all mistakes

     [:div
      {:class ["pt-4"]}
      [components.debug/raw-metadata game]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cluster [opts games]
  (when (seq games)
    [:div
     {:class ["flex" "flex-col"]}
     (for [game games]
       [:div
        {:key   (:lichess.game/id game)
         :class []}

        [floating/popover
         {:offset 5
          :click  true
          :anchor-comp
          [:div
           {:class ["cursor-pointer"]}
           [thumbnail opts game]]
          :popover-comp
          [detail-popover opts game]}]])]))
