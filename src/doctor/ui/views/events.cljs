(ns doctor.ui.views.events
  (:require
   [tick.core :as t]
   [doctor.ui.events :as events]
   [doctor.ui.views.screenshots :as screenshots]
   [doctor.ui.components.debug :as debug]
   [clojure.string :as string]))


(defn ->string [_opts it]
  [:div "event" (str it)])

(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format
      (t/formatter "MMM dd HH:mm")
      (:event/timestamp it))))

(comment
  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes)))

  (some->
    (re-seq #"(\w+/\w+)$" "/users/russ/blah/hello")
    first
    first
    )
  )

(defn short-repo [it]
  (some->
    (re-seq #"(\w+/\w+)$" (:git.commit/directory it))
    first
    first))

(defn commit-comp [opts it]
  (let [{:git.commit/keys
         [body subject short-hash]} it
        short-dir                   (short-repo it)
        body-lines
        (->
          body
          (string/split "\n"))]
    [:div
     {:class ["flex" "flex-col"]}

     [:div
      {:class ["flex" "flex-row"]}
      [:div
       {:class ["text-xl"
                "text-city-green-400"]}
       subject]

      [:div
       {:class ["flex" "flex-col" "ml-auto"]}
       [:div
        {:class ["text-city-pink-400"]}
        short-hash]

       [:div
        {:class ["text-city-pink-400"]}
        short-dir]]]

     [:div
      {:class ["pt-8"]}
      (for [[idx line] (->> body-lines
                            (map-indexed vector))]
        ^{:key idx}
        [:p line])]]))


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

    (when (:git.commit/hash it)
      [:div
       {:class ["w-3/5"]}
       [commit-comp opts it]])

    [:div
     {:class ["flex" "px-8" "w-1/3" "lg:px-16" "lg:w-1/5"]}
     [debug/raw-metadata (-> opts
                             (assoc :label "Event metadata")
                             (assoc :initial-show? false)
                             (assoc :exclude-key #{:git.commit/body
                                                   :git.commit/full-message})) it]

     (when opts [debug/raw-metadata (-> opts
                                        (assoc :label "And opts")
                                        (assoc :initial-show? true)) opts])]]])


(defn event-page []
  (let [{:keys [items]} (events/use-events)]
    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"
              "p-6"]}
     ;; TODO filter by type (screenshots, commits, org items)
     [:div [:h1 {:class ["pb-4" "text-xl"]} (->> items count) " Events"]]

     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [event-comp nil it])]))
