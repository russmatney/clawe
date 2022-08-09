(ns pages.db.tables
  (:require
   [tick.core :as t]
   [components.table :as components.table]
   [components.floating :as floating]
   [components.debug :as components.debug]
   [components.garden :as components.garden]
   [components.wallpaper :as components.wallpaper]
   [components.chess :as components.chess]
   [components.screenshot :as components.screenshot]) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; table defs
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

(defn table-for-doctor-type [doctor-type entities]
  [components.table/table
   (cond
     (#{:type/garden} doctor-type)
     {:headers ["File" "Name" "Parent" "Words" "Raw"]
      :rows    (->>
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
                          note]])))}

     (#{:type/wallpaper} doctor-type)
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
                             wp]])))}

     (#{:type/screenshot} doctor-type)
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
                             scr]])))}

     (#{:type/lichess-game} doctor-type)
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
                             game]])))}

     (#{:type/repo} doctor-type)
     {:headers ["Repo" "Raw"]
      :rows    (->> entities
                    (sort-by :db/id)
                    (reverse)
                    (take 10)
                    (map (fn [repo]
                           [(-> repo :repo/short-path)
                            [components.debug/raw-metadata
                             {:label "raw"}
                             repo]])))}

     (#{:type/commit} doctor-type)
     {:headers ["Subject" "Raw"]
      :rows    (->> entities
                    (take 3)
                    (map (fn [commit]
                           [(:commit/subject commit)
                            [components.debug/raw-metadata
                             {:label "raw"}
                             commit]])))}


     :else
     ;; fallback to table of keys/vals
     (let [first-ent (-> entities first)
           headers   (-> first-ent keys)]
       {:headers (concat ["Raw"] (map str headers))
        :rows    (->> entities
                      (take 3)
                      (map (fn [ent]
                             (concat
                               [[components.debug/raw-metadata {:label "raw"} ent]]
                               (->> headers
                                    (map (fn [h]
                                           (str (get ent h)))))))))}))])
