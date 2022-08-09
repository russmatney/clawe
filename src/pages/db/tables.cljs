(ns pages.db.tables
  (:require
   [hooks.db :as hooks.db]
   [dates.tick :as dates.tick]
   [tick.core :as t]
   [components.table :as components.table]
   [components.floating :as floating]
   [components.debug :as components.debug]
   [components.garden :as components.garden]
   [components.wallpaper :as components.wallpaper]
   [components.chess :as components.chess]
   [components.screenshot :as components.screenshot]
   [components.git :as components.git]
   [doctor.ui.db :as ui.db]))

(defn basic-text-popover [text]
  [:div
   {:class
    ["text-city-blue-400"
     "flex" "flex-col" "p-2"
     "bg-yo-blue-500"]}
   text])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden tables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn garden-by-tag-table-def [entities]
  {:headers ["Tag" "Count" "Example"]
   :rows    (->>
              entities
              (group-by :org/tags)
              (sort-by (comp count second))
              reverse
              (take 10) ;; only take the top 10 tags
              (map (fn [[tag group]]
                     [[:span
                       (or tag "(no tag)")]
                      [:span
                       {:class ["font-nes"]}
                       (count group)]
                      [components.debug/raw-metadata
                       {:label (-> group first :org/name)}
                       (first group)]])))} )

(defn garden-note-table-def [entities]
  {:headers ["File" "Name" "Parent" "Words" "Raw"]
   :rows
   (->>
     entities
     (sort-by :file/last-modified) ;; TODO this is per file, not per item
     reverse
     (take 10) ;; only take the top 10 tags
     (map (fn [note]
            [(:org/short-path note)
             (:org/name note)
             (:org/parent-name note)
             [floating/popover
              {:hover true :click true
               :anchor-comp
               [:span
                {:class [(when (seq (:org/body-string note))
                           "text-city-pink-400")]}
                (:org/word-count note)]
               :popover-comp
               [:div
                {:class
                 ["text-city-blue-400"
                  "flex" "flex-col" "p-2"
                  "bg-yo-blue-500"]}
                (:org/name note)
                [components.garden/org-body note]]}]
             [components.debug/raw-metadata
              {:label "raw"}
              note]])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpaper/screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wallpaper-table-def [entities]
  {:headers ["Img" "Wallpaper" "Used count" "Last Time Set" "Raw"]
   :rows    (->> entities
                 (sort-by :wallpaper/last-time-set)
                 (reverse)
                 (take 5)
                 (map (fn [wp]
                        [[floating/popover
                          {:hover true :click true
                           :anchor-comp
                           [:img {:src   (-> wp :file/web-asset-path)
                                  :class ["max-h-24"]}]
                           :popover-comp
                           [components.wallpaper/wallpaper-comp wp]}]
                         (-> wp :wallpaper/short-path)
                         (:wallpaper/used-count wp)
                         (-> wp :wallpaper/last-time-set (t/new-duration :millis) t/instant)

                         [components.debug/raw-metadata
                          {:label "raw"}
                          wp]])))})

(defn screenshot-table-def [entities]
  {:headers ["Img" "Name" "Time" "Raw"]
   :rows    (->> entities
                 (sort-by :screenshot/time)
                 (reverse)
                 (take 5)
                 (map (fn [scr]
                        [[components.screenshot/cluster-single nil scr]
                         (-> scr :name)
                         (-> scr :screenshot/time (t/new-duration :millis) t/instant)

                         [components.debug/raw-metadata
                          {:label "raw"}
                          scr]])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; lichess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn lichess-game-table-def [entities]
  {:headers ["" "Opening" "Created at" "Raw"]
   :rows    (->> entities
                 (sort-by :lichess.game/created-at)
                 (reverse)
                 (take 5)
                 (map (fn [{:lichess.game/keys [] :as game}]
                        [[components.chess/cluster-single nil game]
                         (-> game :lichess.game/opening-name)
                         (-> game :lichess.game/created-at (t/new-duration :millis) t/instant)
                         [components.debug/raw-metadata
                          {:label "raw"}
                          game]])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-table-def
  ([repos] (repo-table-def nil repos))
  ([{:keys [conn]} repos]
   {:headers ["Repo" "Commits (in db)" "Ingest" "Raw"]
    :rows    (->> repos
                  (sort-by :db/id)
                  (reverse)
                  (take 10)
                  (map (fn [repo]
                         [(-> repo :repo/short-path)
                          (let [commits (ui.db/commits-for-repo conn repo)]
                            (count commits))
                          [:button {:class    ["bg-slate-600" "p-4" "rounded-xl"]
                                    :on-click (fn [_]
                                                (hooks.db/ingest-commits-for-repo repo))}
                           "Ingest latest commits"]
                          [components.debug/raw-metadata
                           {:label "raw"}
                           repo]])))}))

(defn commit-table-def
  ([commits] (commit-table-def nil commits))
  ([{:keys [conn]} commits]
   {:headers ["Hash" "Subject" "Added/Removed" "Repo" "Raw"]
    :rows    (->> commits
                  (sort-by (comp dates.tick/parse-time-string :commit/author-date) t/>)
                  (take 10)
                  (map (fn [commit]
                         (let [repo (ui.db/repo-for-commit conn commit)]
                           [[components.git/short-hash-link commit repo]
                            (if (seq (:commit/body commit))
                              [floating/popover
                               {:hover        true :click true
                                :anchor-comp  (:commit/subject commit)
                                :popover-comp [basic-text-popover (:commit/full-message commit)]}]
                              (:commit/subject commit))
                            [components.git/added-removed commit]
                            (if repo (:repo/short-path repo)
                                (:commit/directory commit))
                            [components.debug/raw-metadata
                             {:label "raw"}
                             commit]]))))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-for-doctor-type
  ([type entities] (table-for-doctor-type nil type entities))
  ([opts doctor-type entities]
   [components.table/table
    (cond
      (#{:type/garden} doctor-type)
      (garden-note-table-def entities)

      (#{:type/wallpaper} doctor-type)
      (wallpaper-table-def entities)

      (#{:type/screenshot} doctor-type)
      (screenshot-table-def entities)

      (#{:type/lichess-game} doctor-type)
      (lichess-game-table-def entities)

      (#{:type/repo} doctor-type)
      (repo-table-def opts entities)

      (#{:type/commit} doctor-type)
      (commit-table-def opts entities)

      :else
      ;; fallback to table of keys/vals
      (let [first-ent (-> entities first)
            headers   (->> first-ent keys
                           ;; only show a few keys to prevent super-wide table
                           (take 3))]
        {:headers (concat ["Raw"] (map str headers))
         :rows    (->> entities
                       (take 3)
                       (map (fn [ent]
                              (concat
                                [[components.debug/raw-metadata {:label "raw"} ent]]
                                (->> headers
                                     (map (fn [h]
                                            (str (get ent h)))))))))}))]))
