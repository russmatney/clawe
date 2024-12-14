(ns components.chess
  (:require
   [clojure.string :as string]
   [goog.string :as gstring]
   [goog.string.format]
   [wing.core :as w]
   [uix.core :as uix :refer [$ defui]]
   ["chessground" :refer [Chessground]]
   ["chess.js" :as Chess]

   [components.floating :as floating]
   [components.debug]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; chessground wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui chessground [{:keys [shapes autoShapes] :as opts}]
  (let [opts                (clj->js (dissoc opts :shapes :autoShapes))
        [cg-api set-cg-api] (uix/use-state nil)
        ref                 (uix/use-ref)]

    ;; set/destroy cg-api
    (uix/use-effect
      (fn []
        (when (and (not cg-api) ref)
          (set-cg-api (Chessground. ref opts)))
        #(when cg-api (.destroy cg-api)))
      [cg-api])

    ;; set config once api is set
    (uix/use-effect
      (fn []
        (when cg-api
          (.set cg-api opts)
          ;; set shapes cannot be combined with :fen, so we call it after
          ;; https://github.com/lichess-org/chessground/issues/171
          (when shapes
            (.setShapes cg-api (clj->js shapes)))
          (when autoShapes
            (.setAutoShapes cg-api (clj->js autoShapes)))))
      [cg-api opts shapes autoShapes])

    ($ :div {:class ["w-full" "h-full"] :ref ref})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; thumbnail
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui player-and-rating [{:keys [player rating-diff]}]
  ($ :div
     {:class ["flex" "flex-row"
              "gap-x-1"]}
     ($ :span
        {:class ["font-nes"
                 (when (#{"russmatney"} player)
                   "text-city-pink-300")]}
        player)

     (when (#{"russmatney"} player)
       ($ :span
          {:class [(cond
                     (> rating-diff 0) "text-city-green-400"
                     (< rating-diff 0) "text-city-red-500"
                     :else             "text-city-gray-500")]}
          (when (> rating-diff 0)
            "+")
          rating-diff))))

(defui thumbnail [{:keys [game]}]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf analysis]} game]
    ($ :div
       {:class ["py-2"]}

       ($ :div
          {:class ["flex" "flex-row"
                   "items-end"
                   "gap-x-3"
                   "font-mono"]}
          ($ :span
             {:class ["capitalize"]}
             perf)

          ($ player-and-rating {:player white-player :rating-diff white-rating-diff})
          ($ :span "vs")
          ($ player-and-rating {:player black-player :rating-diff black-rating-diff})

          (when (seq analysis)
            ($ :span
               {:class ["text-sm"]}
               "[Analysis available]"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mouse wheel scroll prevention
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; https://stackoverflow.com/a/55543526/860787

(defn prevent-default [e]
  (let [e (or e js/window.event)]
    (when (.-preventDefault e)
      (.preventDefault e))))

(defn enable-scroll []
  (js/document.removeEventListener "wheel" prevent-default false))

(defn disable-scroll []
  (js/document.addEventListener "wheel" prevent-default #js {:passive false}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; display game state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn display-game-state
  [{:keys [all-game-states i game]}]
  (let [max-i                           (dec (count all-game-states))
        [state-cursor set-state-cursor] (uix/use-state i)
        go-to-prev-state                #(set-state-cursor
                                           (fn [current] (max 0 (dec current))))
        go-to-next-state                #(set-state-cursor
                                           (fn [current] (min max-i (inc current))))

        {:keys [fen move best eval eval-diff mate judgment move-number]}
        nil
        ;; TODO restore this!
        #_ (nth all-game-states state-cursor nil)

        {:lichess.game/keys [white-player]} game
        ;; wheel-container-ref                 (uix/ref)
        ]

    ($ :div
       {:class ["flex" "flex-col" "w-64" "items-center"]}
       ($ :div
          {:class ["flex" "flex-row"
                   "w-full"
                   "h-12"
                   "items-center"]}
          ($ :span {:class ["font-mono" "whitespace-nowrap"]}
             (str (string/replace (str move-number) ".5" "..") ". " (get move "san")))

          (when (or mate eval-diff)
            ($ :span
               {:class
                (concat ["ml-auto" "font-nes"]
                        (cond
                          mate              ["text-2xl" "text-city-red-500"
                                             "animate__animated" "animate__tada"
                                             "animate__repeat-3"]
                          (< eval-diff 100) ["text-sm" "text-city-blue-500"]
                          (> eval-diff 700) ["text-2xl" "text-city-red-600"
                                             "animate__animated" "animate__tada"
                                             "animate__repeat-2"]
                          (> eval-diff 600) ["text-xl" "text-city-red-500"
                                             "animate__animated" "animate__tada"
                                             "animate__repeat-1"]
                          (> eval-diff 500) ["text-xl" "text-city-red-400"]
                          (> eval-diff 400) ["text-xl" "text-city-pink-500"]
                          (> eval-diff 300) ["text-xl" "text-city-pink-400"]
                          (> eval-diff 200) ["text-xl" "text-city-pink-300"]
                          (> eval-diff 100) ["text-lg" "text-city-pink-200"]
                          :else             []))}
               (if mate
                 "Mate"
                 (gstring/format "%.1f" (/ eval-diff 100)))))

          (when eval
            ($ :span
               {:class (concat ["ml-auto" "font-nes" "decoration-4" "underline"]
                               (cond
                                 (< eval 0) ["text-city-black-300"]
                                 (> eval 0) ["text-city-pink-100"]
                                 :else      []))}
               (str (when (> eval 0) "+") (gstring/format "%.1f" (/ eval 100)))))

          (when mate
            ($ :span
               {:class ["ml-auto" "font-nes"]}
               (str "#" mate))))

       ($ :div
          {:class          ["w-64" "h-64"]
           ;; :ref            wheel-container-ref
           :on-mouse-enter #(disable-scroll)
           :on-mouse-leave #(enable-scroll)
           :on-wheel       (fn [ev]
                             (if (> (.-deltaY ev) 0)
                               (go-to-next-state)
                               (go-to-prev-state)))}
          ($ chessground
             {:fen              fen
              :lastMove         #js [(get move "from") (get move "to")]
              :blockTouchScroll true
              ;; :viewOnly    true
              :coordinates      false
              :orientation      (if (#{"russmatney"} white-player)
                                  "white" "black")
              :width            "220px"
              :height           "220px"
              :autoShapes       (->> [(when best
                                        (let [[orig dest] (->> best (partition 2 2)
                                                               (map #(apply str %)))]
                                          {:orig orig :dest dest :brush "blue"}))]
                                     (remove nil?))}))

       (when judgment ($ :span {:class ["font-mono"]}
                         ;; the name is sometimes repeated in the comment, so we
                         ;; remove it
                         (str (:name judgment) ".")
                         (->
                           (:comment judgment)
                           (string/replace (str (:name judgment) ".") "")))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; game-state list
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

(defn build-game-states [game]
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
                     {:game        game
                      :i           i
                      :fen         f
                      :move        (js->clj move)
                      :move-number (+ 0.5 (/ (inc i) 2))}
                     analysis))))
             (reduce (fn [acc next]
                       (let [lst (when (seq acc) (last acc))]
                         (if lst
                           (conj acc
                                 (cond
                                   (:eval next)
                                   (assoc next :eval-diff (js/Math.abs (- (:eval lst) (:eval next))))

                                   :else next))
                           (conj acc next))))
                     []))]
    (->> all-game-states
         ;; attach this list to each state as well
         (map #(assoc % :all-game-states all-game-states)))))

(defui display-game-states
  [{:keys [game-states]}]
  ($ :div
     {:class ["grid" "grid-cols-4" "gap-2" "gap-x-8"]}
     (for [[i game-state] (->> game-states (map-indexed vector))]
       ($ display-game-state {:key             i :i i
                              :all-game-states game-states
                              :game            game-state}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; detail-popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui detail-popover [{:keys [game]}]
  (let [{:lichess.game/keys
         [white-player black-player
          white-rating-diff black-rating-diff
          perf opening-name moves
          url analysis]}                          game
        [show-all-mistakes set-show-all-mistakes] (uix/use-state nil)
        all-game-states                           (build-game-states game)]

    ($ :div
       {:class ["bg-yo-blue-500" "p-3"
                "text-city-blue-200"
                "flex" "flex-col"]}

       ($ :div
          {:class ["flex" "flex-row"]}
          ($ :div
             {:class ["flex" "flex-row"
                      "items-center"
                      "gap-x-3"
                      "font-mono"]}
             ($ :span
                {:class ["capitalize"]}
                perf)

             ($ player-and-rating {:player white-player :rating-diff white-rating-diff})
             ($ :span "vs")
             ($ player-and-rating {:player black-player :rating-diff black-rating-diff}))
          ($ :div
             {:class ["ml-auto"]}

             ($ :div
                ($ :a
                   {:href   url
                    :target "_blank"
                    :class  ["hover:city-pink-400"
                             "city-pink-200"
                             "cursor-pointer"]}
                   "View on lichess"))))

       ($ :div
          {:class ["text-xl"]}
          opening-name)

       ($ :div
          {:class []}
          (str (/ (count (string/split moves #" ")) 2) " moves"))

       ;; highlights
       ($ display-game-states {:game-states (default-game-state-filter all-game-states)})

       ;; all mistakes
       (when (seq analysis)
         ($ :span {:on-click #(set-show-all-mistakes not)
                   :class    ["text-city-pink-200"
                              "hover:text-city-pink-400"
                              "cursor-pointer"]}
            "Toggle all mistakes"))

       (when (and (seq analysis) show-all-mistakes)
         ($ display-game-states {:game-states (->> all-game-states
                                                   (filter :judgment)
                                                   (sort-by (comp js/Math.abs :eval-diff) >)
                                                   (sort-by (comp not :mate)))}))

       ($ :div
          {:class ["pt-4"]}
          ($ components.debug/raw-metadata {:data game})))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cluster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui cluster-single [opts]
  ($ :div
     ($ floating/popover
        {:offset 5
         :click  true
         :anchor-comp
         ($ :div
            {:class ["cursor-pointer"]}
            ($ thumbnail opts))
         :popover-comp
         ($ detail-popover opts)})))

(defui cluster [{:keys [games] :as opts}]
  (when (seq games)
    ($ :div
       {:class ["flex" "flex-col"]}
       (for [game games]
         ($ cluster-single
            (assoc opts
                   :game game
                   :key (:lichess.game/id game)))))))
