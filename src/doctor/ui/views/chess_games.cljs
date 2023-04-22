(ns doctor.ui.views.chess-games
  (:require
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]
   [components.chess :as components.chess]
   [components.actions :as components.actions]))

(defn actions []
  [{:action/label    "Ingest lichess games"
    :action/on-click (fn [_] (handlers/ingest-lichess-games))}])

(defn widget [opts]
  (let [games (ui.db/chess-games (:conn opts))]
    [:div
     {:class ["flex" "flex-col" "w-full"]}

     [:div
      {:class ["flex" "flex-row" "items-center" "justify-end"]}
      [components.actions/actions-list (actions)]]

     [:div
      {:class ["p-4"
               "text-slate-200"]}
      (for [game games]
        ^{:key (:lichess.game/id game)}
        [:div
         {:class ["py-2"
                  "flex" "flex-row"
                  "items-center" "justify-between"]}
         [components.chess/cluster-single nil game]
         [components.actions/actions-list (handlers/->actions game)]])]]))
