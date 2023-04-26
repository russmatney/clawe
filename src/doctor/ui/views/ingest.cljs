(ns doctor.ui.views.ingest
  (:require
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]
   [components.actions :as components.actions]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ingest buttons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ingest-actions []
  [{:action/label    "Reingest todos"
    :action/on-click (fn [_] (handlers/reingest-todos))}
   {:action/label    "Ingest full garden"
    :action/on-click (fn [_] (handlers/ingest-garden-full))}
   {:action/label    "Ingest garden latest"
    :action/on-click (fn [_] (handlers/ingest-garden-latest))}
   {:action/label    "Ingest clawe repos"
    :action/on-click (fn [_] (handlers/ingest-clawe-repos))}
   {:action/label    "Ingest lichess games"
    :action/on-click (fn [_] (handlers/ingest-lichess-games))}
   {:action/label    "Clear lichess cache"
    :action/on-click (fn [_] (handlers/clear-lichess-games-cache))}
   {:action/label    "Ingest screenshots"
    :action/on-click (fn [_] (handlers/ingest-screenshots))}
   {:action/label    "Ingest clips"
    :action/on-click (fn [_] (handlers/ingest-clips))}
   {:action/label    "Ingest wallpapers"
    :action/on-click (fn [_] (handlers/ingest-wallpapers))}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commit ingestion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit-ingest-actions [conn]
  (let [repos (ui.db/repos conn)]
    (->> repos
         (map (fn [repo]
                {:action/label (:repo/short-path repo)
                 :action/on-click
                 (fn [_] (handlers/ingest-commits-for-repo
                           (dissoc repo :actions/inferred)))}))
         (sort-by :action/label))))

(defn commit-ingest-buttons [conn]
  [:div
   [components.actions/actions-list {:actions
                                     (commit-ingest-actions conn)}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ingest buttons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ingest-buttons
  ([] [ingest-buttons nil])
  ([_opts]
   [:div
    {:class ["grid" "grid-cols-6"
             "space-x-2"
             "p-2"]}
    (for [{:action/keys [label on-click]} (ingest-actions)]
      ^{:key label}
      [:button {:class    ["bg-slate-800"
                           "p-4"
                           "border"
                           "border-slate-600"
                           "rounded-xl"
                           "font-mono"
                           "text-white"]
                :on-click on-click}
       label])]))
