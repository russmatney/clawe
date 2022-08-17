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
   [doctor.ui.handlers :as handlers]
   [components.actions :as components.actions]))

(defn basic-text-popover [text]
  [:div
   {:class
    ["text-city-blue-400"
     "flex" "flex-col" "p-2"
     "bg-yo-blue-500"]}
   text])

(defn actions-cell [item]
  [components.actions/actions-popup
   {:actions (handlers/->actions item)}])

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
    {:headers ["File" "Name" "Parent" "Words" "Raw" "Actions"]
     :n       5
     :rows
     (->>
       notes
       (sort-by :file/last-modified) ;; TODO this is per file, not per item
       reverse
       (map (fn [note]
              [(:org/short-path note)
               [components.garden/text-with-links (:org/name note)]
               [components.garden/text-with-links (:org/parent-name note)]
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
                  [components.garden/text-with-links (:org/name note)]
                  [components.garden/org-body note]]}]
               [components.debug/raw-metadata {:label "raw"} note]
               [actions-cell note]])))}))

(defn garden-file-table-def [entities]
  (let [notes (->> entities (filter (comp #{:type/garden} :doctor/type)))]
    {:headers ["File" "Name" "Raw" "Actions"]
     :n       5
     :rows
     (->>
       notes
       (filter (comp #{:level/root} :org/level) )
       (sort-by :file/last-modified >)
       (map (fn [note]
              [[floating/popover
                {:hover        true :click true
                 :anchor-comp  (:org/short-path note)
                 :popover-comp [components.garden/full-note note]}]
               (:org/name note)
               [components.debug/raw-metadata {:label "raw"} note]
               [actions-cell note]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpaper/screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wallpaper-table-def
  ([wps] (wallpaper-table-def nil wps))
  ([opts wps]
   (let [wps (->> wps (filter (comp #{:type/wallpaper} :doctor/type)))]
     {:headers ["Img" "Wallpaper" "Used count" "Last Time Set" "Raw" "Actions"]
      :n       (:n opts 5)
      :rows    (->> wps
                    (sort-by :wallpaper/last-time-set >)
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
                            [components.debug/raw-metadata {:label "raw"} wp]
                            [actions-cell wp]])))})))

(defn screenshot-table-def [entities]
  (let [screenshots (->> entities (filter (comp #{:type/screenshot} :doctor/type)))]
    {:headers ["Img" "Name" "Time" "Raw" "Actions"]
     :n       5
     :rows    (->> screenshots
                   (sort-by :screenshot/time)
                   (reverse)
                   (map (fn [scr]
                          [[components.screenshot/cluster-single nil scr]
                           (-> scr :name)
                           (-> scr :screenshot/time (t/new-duration :millis) t/instant)
                           [components.debug/raw-metadata {:label "raw"} scr]
                           [actions-cell scr]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; lichess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn lichess-game-table-def [entities]
  (let [games (->> entities (filter (comp #{:type/lichess-game} :doctor/type)))]
    {:headers ["" "Opening" "Created at" "Raw" "Actions"]
     :n       5
     :rows    (->> games
                   (sort-by :lichess.game/created-at)
                   (reverse)
                   (map (fn [{:lichess.game/keys [] :as game}]
                          [[components.chess/cluster-single nil game]
                           (-> game :lichess.game/opening-name)
                           (-> game :lichess.game/created-at (t/new-duration :millis) t/instant)
                           [components.debug/raw-metadata {:label "raw"} game]
                           [actions-cell game]])))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-table-def
  ([repos] (repo-table-def nil repos))
  ([{:keys [conn]} repos]
   (let [repos (->> repos (filter (comp #{:type/repo} :doctor/type)))]
     {:headers ["Repo" "Commits (in db)" "Raw" "Actions"]
      :n       5
      :rows    (->> repos
                    (sort-by :db/id)
                    (reverse)
                    (map (fn [repo]
                           [(-> repo :repo/short-path)
                            (let [commits (ui.db/commits-for-repo conn repo)]
                              (count commits))
                            [components.debug/raw-metadata {:label "raw"} repo]
                            [actions-cell repo]])))})))

(defn commit-table-def
  ([commits] (commit-table-def nil commits))
  ([{:keys [conn]} commits]
   (let [commits (->> commits (filter (comp #{:type/commit} :doctor/type)))]
     {:headers ["Hash" "Subject" "Added/Removed" "Repo" "Raw" "Actions"]
      :n       5
      :rows    (->> commits
                    (sort-by (comp dates.tick/parse-time-string :commit/author-date) t/>)
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
                              [components.debug/raw-metadata {:label "raw"} commit]
                              [actions-cell commit]]))))})))

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
     {:headers (concat ["Raw" "Actions"] (map str headers))
      :n       3
      :rows    (->> entities
                    (map (fn [ent]
                           (concat
                             [[components.debug/raw-metadata {:label "raw"} ent]
                              [actions-cell ent]]
                             (->> headers
                                  (map (fn [h]
                                         (str (get ent h)))))))))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-table-defs [opts entities]
  ;; TODO how to use fallback table?
  [(summary-table-def entities)
   (garden-by-tag-table-def entities)
   (garden-note-table-def entities)
   (garden-file-table-def entities)
   (wallpaper-table-def opts entities)
   (screenshot-table-def entities)
   (lichess-game-table-def entities)
   (repo-table-def opts entities)
   (commit-table-def opts entities)])

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
