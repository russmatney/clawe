(ns pages.db.tables
  (:require
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
   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]))

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
  (let [notes (->> entities (filter (comp #{:type/garden} :doctor/type)))]
    {:headers ["Tag" "Count" "Example"]
     :rows    (->>
                notes
                (group-by :org/tags)
                ;; TODO explode counts per tag instead of tag-group, like in comp.filter
                (sort-by (comp count second))
                reverse
                (take 10) ;; only take the top 10 tags
                (map (fn [[tags group]]
                       [[:span
                         (if (seq tags) (str tags) "(no tag)")]
                        [:span
                         {:class ["font-nes"]}
                         (count group)]
                        [components.debug/raw-metadata
                         {:label (-> group first :org/name)}
                         (first group)]])))}) )

(defn garden-note-table-def [entities]
  (let [notes (->> entities (filter (comp #{:type/garden} :doctor/type)))]
    {:headers ["File" "Name" "Parent" "Words" "Raw"]
     :rows
     (->>
       notes
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
                note]])))}))

(defn garden-file-table-def [entities]
  (let [notes (->> entities (filter (comp #{:type/garden} :doctor/type)))]
    {:headers ["File" "Name" "Raw"]
     :rows
     (->>
       notes
       (filter (comp #{:level/root} :org/level) )
       (sort-by :file/last-modified >)
       (take 10) ;; only take the 10 most recent
       (map (fn [note]
              [[floating/popover
                {:hover        true :click true
                 :anchor-comp  (:org/short-path note)
                 :popover-comp [components.garden/full-note-popover note]}]
               (:org/name note)
               [components.debug/raw-metadata
                {:label "raw"}
                note]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpaper/screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wallpaper-table-def
  ([wps] (wallpaper-table-def nil wps))
  ([opts wps]
   (let [wps (->> wps (filter (comp #{:type/wallpaper} :doctor/type)))]
     {:headers ["Img" "Wallpaper" "Used count" "Last Time Set" "Raw"]
      :rows    (->> wps
                    (sort-by :wallpaper/last-time-set >)
                    (take (:n opts 5))
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
                            (some-> wp :wallpaper/last-time-set
                                    (t/new-duration :millis) t/instant)

                            [components.debug/raw-metadata
                             {:label "raw"}
                             wp]])))})))

(defn screenshot-table-def [entities]
  (let [screenshots (->> entities (filter (comp #{:type/screenshot} :doctor/type)))]
    {:headers ["Img" "Name" "Time" "Raw"]
     :rows    (->> screenshots
                   (sort-by :screenshot/time)
                   (reverse)
                   (take 5)
                   (map (fn [scr]
                          [[components.screenshot/cluster-single nil scr]
                           (-> scr :name)
                           (-> scr :screenshot/time (t/new-duration :millis) t/instant)

                           [components.debug/raw-metadata
                            {:label "raw"}
                            scr]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; lichess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn lichess-game-table-def [entities]
  (let [games (->> entities (filter (comp #{:type/lichess-game} :doctor/type)))]
    {:headers ["" "Opening" "Created at" "Raw"]
     :rows    (->> games
                   (sort-by :lichess.game/created-at)
                   (reverse)
                   (take 5)
                   (map (fn [{:lichess.game/keys [] :as game}]
                          [[components.chess/cluster-single nil game]
                           (-> game :lichess.game/opening-name)
                           (-> game :lichess.game/created-at (t/new-duration :millis) t/instant)
                           [components.debug/raw-metadata
                            {:label "raw"}
                            game]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-table-def
  ([repos] (repo-table-def nil repos))
  ([{:keys [conn]} repos]
   (let [repos (->> repos (filter (comp #{:type/repo} :doctor/type)))]
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
                                                  (handlers/ingest-commits-for-repo repo))}
                             "Ingest latest commits"]
                            [components.debug/raw-metadata
                             {:label "raw"}
                             repo]])))})))

(defn commit-table-def
  ([commits] (commit-table-def nil commits))
  ([{:keys [conn]} commits]
   (let [commits (->> commits (filter (comp #{:type/commit} :doctor/type)))]
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
                               commit]]))))})))

(defn summary-table-def [entities]
  (let [ents-by-doctor-type (->> entities (group-by :doctor/type))]
    {:headers [":doctor/type"
               "Entities"
               "Keys counts"]
     :rows    (concat
                ;; first row is special, has all
                [["all" (count entities) "n/a"]]
                (map (fn [[type entities]]
                       [(str type)
                        (count entities)
                        (->> entities
                             (map (comp count keys))
                             (map #(str % " "))
                             (distinct)
                             (take 5)
                             (apply str ))])
                     ents-by-doctor-type))}))

(defn fallback-table-def
  ([ents] (fallback-table-def nil ents))
  ([_opts entities]
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
                                         (str (get ent h)))))))))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-table-defs [opts entities]
  [(summary-table-def entities)
   (garden-by-tag-table-def entities)
   (garden-note-table-def entities)
   (garden-file-table-def entities)
   (wallpaper-table-def opts entities)
   (screenshot-table-def entities)
   (lichess-game-table-def entities)
   (repo-table-def opts entities)
   (commit-table-def opts entities)

   ;; TODO how to work in fallback table?
   ]
  )

(defn table-def-for-doctor-type
  ([type entities] (table-def-for-doctor-type nil type entities))
  ([opts doctor-type entities]
   (cond
     (#{:type/garden} doctor-type)
     (garden-note-table-def entities)

     (#{:type/wallpaper} doctor-type)
     (wallpaper-table-def opts entities)

     (#{:type/screenshot} doctor-type)
     (screenshot-table-def entities)

     (#{:type/lichess-game} doctor-type)
     (lichess-game-table-def entities)

     (#{:type/repo} doctor-type)
     (repo-table-def opts entities)

     (#{:type/commit} doctor-type)
     (commit-table-def opts entities)

     :else (fallback-table-def opts entities))))

(defn table-for-doctor-type
  ([type entities] (table-for-doctor-type nil type entities))
  ([opts doctor-type entities]
   [components.table/table (table-def-for-doctor-type opts doctor-type entities)]))
