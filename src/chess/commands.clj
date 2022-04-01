(ns chess.commands
  (:require
   [defthing.defcom :refer [defcom]]

   [ralphie.notify :as notify]
   [ralphie.rofi :as rofi]
   [ralphie.browser :as browser]

   [chess.core :as chess]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; chess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom open-chess-game
  (do
    (notify/notify "Fetching chess games")
    (->>
      (chess/fetch-games)
      (map (fn [{:keys [white-user black-user] :as game}]
             (assoc game
                    :rofi/label (str white-user " vs " black-user))))
      (rofi/rofi
        {:msg       "Open Game"
         :on-select (fn [{:keys [lichess/url]}]
                      (notify/notify "Opening game" url)
                      (browser/open {:browser.open/url url}))}))))

(defcom clear-lichess-cache
  (do
    (chess/clear-cache)
    (notify/notify "Lichess cache cleared")))
