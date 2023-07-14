(ns blog.pages.last-modified
  (:require
   [tick.core :as t]
   [dates.tick :as dates]

   [blog.item :as item]
   [blog.render :as render]
   [blog.db :as blog.db]))

;; data

(defn notes-by-day [notes]
  (->> notes
       (filter :file/last-modified)
       (map #(dissoc % :org/body))
       (group-by #(-> % :file/last-modified dates/parse-time-string t/date))
       (map (fn [[k v]] [k (into [] v)]))))

(defn day->anchor [day]
  (str day))

(defn day->label [day]
  (str
    (t/format (t/formatter "EEEE") day)
    " "
    (t/format (t/formatter "MMMM") day)
    " "
    (t/format (t/formatter "dd") day)))

(comment
  (day->label (dates/now)))

(defn anchor-groups-by-month [day->notes]
  (->> day->notes
       (map first)
       (remove nil?)
       (group-by #(-> % dates/parse-time-string t/year-month))
       (sort-by first t/>)
       (map (fn [[year-month days]]
              [year-month
               (->> days
                    (sort t/>)
                    (map (fn [day]
                           ;; build for anchor-href-list
                           {:href  (str "/last-modified.html#" (day->anchor day))
                            :label (str (day->label day) " (" (count (day->notes day)) ")")})))]))))

(defn data []
  (let [by-day (->> (blog.db/published-notes) notes-by-day)]
    {:by-day     (->> by-day (sort-by first t/>))
     :day-groups (->> by-day (into {}) anchor-groups-by-month)}))

(comment
  (-> (blog.db/published-notes)
      notes-by-day)
  (:day-groups (data))
  )

;; components

(defn day-block [{:keys [day notes]}]
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h3
     {:class ["pb-2"]}
     [:a     {:class ["cursor-pointer"]
              :id    (day->anchor day)}
      (t/format (t/formatter "EEEE, MMM dd") day)]]]

   (when (seq notes)
     (->> notes (map item/note-row) (into [:div])))
   [:hr]])

(defn day-pool [day-groups]
  (for [[year-month days] day-groups]
    [:div
     {:class ["pt-4"]}
     [:h3 {:class ["flex" "flex-row" "justify-center"]}
      (str (t/month year-month) " " (t/year year-month))]
     ;; TODO could get into mocking a calendar here
     (blog.item/href-pill-list days)]))

(defn page []
  (let [{:keys [by-day day-groups]} (data)]
    [:div
     [:div
      {:class ["flex" "flex-row" "justify-center"]}
      [:h2 {:class ["font-mono"]} "Notes By Date Modified"]]

     (->> by-day
          (map (fn [[day notes]]
                 (when day
                   (day-block {:day day :notes notes}))))
          (into [:div
                 (day-pool day-groups)
                 [:hr]]))]))

(comment
  (render/write-page
    {:path    "/last-modified.html"
     :content (page)
     :title   "By Modified Date"}))
