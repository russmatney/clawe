(ns components.chess
  (:require
   [clojure.string :as string]
   [components.floating :as floating]
   [components.debug]
   [tick.core :as t]

   ["react-chessground" :default Chessground]
   ["chess.js" :as Chess]

   [wing.core :as w]))

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

(comment
  (def pgn
    (->>
      ["[Event \"Casual Game\"]",
       "[Site \"Berlin GER\"]",
       "[Date \"1852.??.??\"]",
       "[EventDate \"?\"]",
       "[Round \"?\"]",
       "[Result \"1-0\"]",
       "[White \"Adolf Anderssen\"]",
       "[Black \"Jean Dufresne\"]",
       "[ECO \"C52\"]",
       "[WhiteElo \"?\"]",
       "[BlackElo \"?\"]",
       "[PlyCount \"47\"]",
       "",
       "1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 4.b4 Bxb4 5.c3 Ba5 6.d4 exd4 7.O-O",
       "d3 8.Qb3 Qf6 9.e5 Qg6 10.Re1 Nge7 11.Ba3 b5 12.Qxb5 Rb8 13.Qa4",
       "Bb6 14.Nbd2 Bb7 15.Ne4 Qf5 16.Bxd3 Qh5 17.Nf6+ gxf6 18.exf6",
       "Rg8 19.Rad1 Qxf3 20.Rxe7+ Nxe7 21.Qxd7+ Kxd7 22.Bf5+ Ke8",
       "23.Bd7+ Kf8 24.Bxe7# 1-0"
       ]
      (string/join "\n"))
    )

  (def chess-inst (Chess/Chess.))

  (def moves (.history chess-inst))

  (.load_pgn chess-inst pgn)
  (.fen chess-inst)

  (for [move (.history chess-inst)]
    (do
      (println "move" move)
      (.move chess-inst move)
      (.fen chess-inst)))

  )

(defn game-highlights [opts game]
  (let [{:lichess.game/keys
         [white-player moves analysis]} game
        chessjs-inst                    (Chess/Chess.)

        board-states (->> (string/split moves #" ")
                          (map-indexed
                            (fn [i move]
                              (let [analysis (nth (or analysis []) i nil)
                                    move     (.move chessjs-inst move)
                                    f        (.fen chessjs-inst)]
                                (merge
                                  {:fen  f
                                   :move (js->clj move)}
                                  analysis)))))

        highlight-states (->> (concat
                                (->> (range (dec (count board-states)))
                                     (filter (comp zero? #(mod % 10)))
                                     (map #(nth board-states % nil)))
                                [(last board-states)])
                              (remove nil?)
                              ;; could be removing valid dupes :shrug:
                              (w/distinct-by identity)
                              (take 3))]

    ;; TODO draw mistake/best-move arrows
    ;; TODO include current eval at each position
    [:div
     {:class ["flex" "flex-row" "gap-2" "flex-wrap"]}

     (for [[i {:keys [fen move
                      best eval]}]
           (->> highlight-states (map-indexed vector))]
       ^{:key i}
       [:div
        [:span {:class ["font-nes" "text-xl"]} eval]
        [:span {:class ["font-nes" "text-xl"]} best]
        [:> Chessground
         {:fen           fen
          :lastMove      #js [(get move "from") (get move "to")]
          ;; :viewOnly    true
          :coordinates   false
          :orientation   (if (#{"russmatney"} white-player)
                           "white" "black")
          :width         "220px"
          :height        "220px"
          :setAutoShapes (fn [x]
                           (println "setShapes x" x)
                           (clj->js [(clj->js {:orig "e4" :dest "e7" :brush "green"})]))}
         ]])]))

(defn detail-popover [opts game]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf opening-name moves
          url analysis
          created-at
          ]} game]
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
         {:href  url
          :class ["hover:city-pink-400"
                  "city-pink-200"
                  "cursor-pointer"]}
         "View on lichess"]]]]

     [:div
      {:class ["text-xl"]}
      opening-name]

     [:div
      {:class []}
      (str (/ (count (string/split moves #" ")) 2) " moves")]

     [game-highlights opts game]

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
         {:hover true
          :click true
          :anchor-comp
          [:div
           {:class ["cursor-pointer"]}
           [thumbnail opts game]]
          :popover-comp
          [detail-popover opts game]
          }]])]))
