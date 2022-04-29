(ns doctor.ui.views.events
  (:require
   [tick.core :as t]

   [doctor.ui.events :as events]
   [doctor.ui.views.screenshots :as screenshots]
   [doctor.ui.components.debug :as debug]))


(defn ->string [_opts it]
  [:div "event" (str it)])

(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format
      (t/formatter "MMM dd HH:mm")
      (:event/timestamp it))))

(comment
  (t/time)

  (let [some-t (t/- (t/now) 5 :hours)]
    (t/ago (t/- (t/now) some-t)))

  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes)))
  )

(defn event-comp [opts it]
  [:div
   {:class ["pt-4" "pb-16"]}
   (when-let [ts (:event/timestamp it)]
     [:div
      {:class ["text-2xl" "pb-4"]}
      (if-let [time-str (->date-str opts it)]
        time-str
        ts)])

   [:div
    {:class ["flex" "flex-row"]}
    (when (:file/web-asset-path it)
      [:div
       {:class ["w-2/5"]}
       [screenshots/screenshot-comp opts it]])

    [:div
     {:class ["flex" "px-16"]}
     [debug/raw-metadata (-> opts
                             (assoc :label "Event metadata")
                             (assoc :initial-show? true)) it]


     (when opts [debug/raw-metadata (-> opts
                                        (assoc :label "And opts")
                                        (assoc :initial-show? true)) opts])]]])


(defn event-page []
  (let [{:keys [items]} (events/use-events)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"
              "p-6"]}
     [:div [:h1 {:class ["pb-4" "text-xl"]} (->> items count) " Events"]]

     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [event-comp nil it])]))
