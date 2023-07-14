(ns blog.pages.date-index
  (:require
   [tick.core :as t]
   [dates.tick :as dates]

   [blog.item :as item]
   [blog.render :as render]
   [blog.db :as blog.db]))

;; data



(defn notes-by-day [{:keys [note->date]} notes]
  (->> notes
       (filter note->date)
       (map #(dissoc % :org/body))
       (group-by #(-> % note->date dates/parse-time-string t/date))
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

(defn anchor-groups-by-month [{:keys [uri]} day->notes]
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
                           {:href  (str uri "#" (day->anchor day))
                            :label (str (day->label day) " (" (count (day->notes day)) ")")})))]))))

(defn data [opts]
  (let [by-day (->> (blog.db/published-notes) (notes-by-day opts))]
    {:by-day     (->> by-day (sort-by first t/>))
     :day-groups (->> by-day (into {}) (anchor-groups-by-month opts))}))

(comment
  (-> (blog.db/published-notes)
      (notes-by-day {:note->date :file/last-modified}))
  (:day-groups (data {:note->date :file/last-modified
                      :uri        "/last-modified.html"}))

  (:day-groups (data {:note->date :org/created-at
                      :uri        "/created-at.html"})))

;; components

(defn day-block [{:keys [uri]} {:keys [day notes]}]
  [:div
   [:div
    {:class ["flex" "flex-row" "justify-center"]}
    [:h3
     {:class ["pb-2"]}
     [:a     {:id (day->anchor day)}
      (t/format (t/formatter "EEEE, MMM dd YYYY") day)]]

    [:a     {:class ["cursor-pointer"
                     "ml-auto"]
             ;; back to top
             :href  (str uri "#")}
     "^top"]]

   (when (seq notes)
     (->> notes (map item/note-row) (into [:div]))) [:hr]])

(defn day-pool [day-groups]
  (for [[year-month days] day-groups]
    [:div
     {:class ["pt-4"]}
     [:h3 {:class ["flex" "flex-row" "justify-center"]}
      (str (t/month year-month) " " (t/year year-month))]
     ;; TODO could get into mocking a calendar here
     (blog.item/href-pill-list days)]))

(defn page [opts]
  (let [{:keys [by-day day-groups]} (data opts)]
    [:div
     [:div
      {:class ["flex" "flex-row" "justify-center"]}
      [:h2 {:class ["font-mono"]} "Notes By Date Modified"]]

     (->> by-day
          (map (fn [[day notes]]
                 (when day
                   (day-block opts {:day day :notes notes}))))
          (into [:div
                 (day-pool day-groups)
                 [:hr]]))]))

(comment
  (render/write-page
    {:path    "/created-at.html"
     :content (page {:note->date :org.prop/created-at
                     :uri        "/created-at.html"})
     :title   "By Date Created"})

  (render/write-page
    {:path    "/published-at.html"
     :content (page {:note->date :blog/published-at
                     :uri        "/published-at.html"})
     :title   "By Date Published"})

  (render/write-page
    {:path    "/last-modified.html"
     :content (page {:uri        "/last-modified.html"
                     :note->date :file/last-modified})
     :title   "By Modified Date"}))
