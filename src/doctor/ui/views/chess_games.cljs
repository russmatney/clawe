(ns doctor.ui.views.chess-games
  (:require
   [uix.core :as uix :refer [$ defui]]

   [components.chess :as components.chess]
   [components.actions :as components.actions]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]))

(defn actions []
  [{:action/label    "Ingest lichess games"
    :action/on-click (fn [_] (handlers/ingest-lichess-games))}])

(defui widget [opts]
  (let [games (ui.db/chess-games (:conn opts))]
    ($ :div
       {:class ["flex" "flex-col" "w-full"]}

       ($ :div
          {:class ["flex" "flex-row" "items-center" "justify-end"]}
          ($ components.actions/actions-list {:actions (actions)}))

       ($ :div
          {:class ["p-4"
                   "text-slate-200"]}
          (for [game games]
            ($ :div
               {:key   (:lichess.game/id game)
                :class ["py-2"
                        "flex" "flex-row"
                        "items-center" "justify-between"]}
               ($ components.chess/cluster-single {:game game})
               ($ components.actions/actions-list {:actions (handlers/->actions game)})))))))
