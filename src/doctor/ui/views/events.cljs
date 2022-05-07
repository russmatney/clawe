(ns doctor.ui.views.events
  (:require
   [tick.core :as t]
   [hooks.events]
   [components.screenshot]
   [components.todo]
   [components.debug]
   [components.git]

   [keybind.core :as key]
   [uix.core.alpha :as uix]))


(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format
      (t/formatter "MMM dd h:mm a")
      (:event/timestamp it))))

(comment
  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes))))

(defn event-comp [{:keys [selected?
                          selected-ref]
                   :as   opts}
                  it]
  [:div
   {:class ["px-4" "pt-4" "pb-16" "flex-col"
            "hover:bg-yo-blue-600"
            (when selected? "bg-yo-blue-500")]
    :ref   selected-ref}
   (when-let [ts (:event/timestamp it)]
     [:div
      {:class ["text-2xl" "pb-4"]}
      (if-let [time-str (->date-str opts it)]
        time-str
        ts)])

   (when (:file/web-asset-path it)
     [:div
      {:class ["w-3/5"]}
      [components.screenshot/screenshot-comp opts it]])

   (when (:git.commit/hash it)
     [:div
      [components.git/commit-comp opts it]])

   (when (:org/name it)
     [:div
      ;; TODO non-todo version of org-item
      [components.todo/todo opts it]])

   [:div
    {:class ["flex" "pt-4"]}
    [components.debug/raw-metadata
     (-> opts
         (assoc :label "Event metadata")
         (assoc :initial-show? false)
         (assoc :exclude-key #{:git.commit/body
                               :git.commit/full-message})) it]]])

(defn event-page []
  (let [{:keys [items]}   (hooks.events/use-events)
        event-count       (count items)
        cursor-idx        (uix/state 0)
        selected-elem-ref (uix/ref)]

    (key/bind! "j" ::cursor-down
               (fn [_ev]
                 (swap! cursor-idx (fn [v]
                                     (let [new-v (inc v)]
                                       (if (> new-v (dec event-count))
                                         (dec event-count)
                                         new-v))))
                 (when selected-elem-ref
                   (println "hi!")
                   (.scrollIntoView @selected-elem-ref))))
    (key/bind! "k" ::cursor-up
               (fn [_ev]
                 (swap! cursor-idx
                        (fn [v]
                          (let [new-v (dec v)]
                            (if (< new-v 0)
                              0 new-v))))
                 (when selected-elem-ref
                   (println "hi!")
                   (.scrollIntoView @selected-elem-ref))))

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"
              "p-6"]}
     ;; TODO filter by type (screenshots, commits, org items)
     [:div [:h1 {:class ["pb-4" "text-xl"]} event-count " Events"]]

     (for [[i it] (->> items (map-indexed vector))]
       (let [selected? (#{i} @cursor-idx)]
         ^{:key i}
         [event-comp
          {:selected?    selected?
           :selected-ref (when selected? selected-elem-ref)}
          it]))]))
