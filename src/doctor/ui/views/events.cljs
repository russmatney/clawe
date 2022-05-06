(ns doctor.ui.views.events
  (:require
   [tick.core :as t]
   [hooks.events :as events]
   [doctor.ui.views.screenshots :as screenshots]
   [doctor.ui.views.todos :as todos]
   [doctor.ui.components.debug :as debug]
   [clojure.string :as string]

   [keybind.core :as key]
   [uix.core.alpha :as uix]))


(defn ->string [_opts it]
  [:div "event" (str it)])

(defn ->date-str [_opts it]
  (when (:event/timestamp it)
    (t/format
      (t/formatter "MMM dd h:mm a")
      (:event/timestamp it))))

(comment
  (t/between
    (t/now)
    (t/>> (t/now) (t/new-duration 10 :minutes)))

  (println "hi")

  (some->
    (re-seq #"(\w+/\w+)$" "/users/russ/blah/hello")
    first
    first))

(defn short-repo [it]
  (some->
    (re-seq #"(\w+/\w+)$" (:git.commit/directory it))
    first
    first))

(defn commit-comp [opts it]
  (let [{:git.commit/keys
         [body subject short-hash hash
          lines-added lines-removed
          ]}      it
        short-dir (short-repo it)
        body-lines
        (->
          body
          (string/split "\n"))]
    [:div
     {:class ["flex" "flex-col"]}

     [:div
      {:class ["flex" "flex-row"]}
      [:div
       {:class ["text-xl" "text-city-green-400"]}
       subject]

      [:div
       {:class ["flex" "flex-col" "ml-auto"]}
       [:a {:class ["text-city-pink-400"
                    "hover:text-city-pink-200"
                    "hover:cursor-pointer"]
            ;; TODO not all commits have public repos (ignore dropbox)
            ;; TODO include this link in db commits
            :href  (str "https://github.com/" short-dir "/commit/" hash)}
        short-hash]

       [:a {:class ["text-city-pink-400"
                    "hover:text-city-pink-200"
                    "hover:cursor-pointer"]
            :href  (str "https://github.com/" short-dir)}
        short-dir]

       [:div
        {:class ["text-city-green-200"]}
        (str "+" lines-added " added")]

       [:div
        {:class ["text-city-red-200"]}
        (str "-" lines-removed " removed")]]]

     [:div
      {:class [(when (seq body-lines) "pt-8")]}
      (for [[idx line] (->> body-lines
                            (map-indexed vector))]
        ^{:key idx}
        [:p line])]]))


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
      [screenshots/screenshot-comp opts it]])

   (when (:git.commit/hash it)
     [:div
      [commit-comp opts it]])

   (when (:org/name it)
     [:div
      ;; TODO non-todo version of org-item
      [todos/todo opts it]])

   [:div
    {:class ["flex" "pt-4"]}
    [debug/raw-metadata
     (-> opts
         (assoc :label "Event metadata")
         (assoc :initial-show? false)
         (assoc :exclude-key #{:git.commit/body
                               :git.commit/full-message})) it]]])

(defn event-page []
  (let [{:keys [items]}   (events/use-events)
        event-count       (count items)
        cursor-idx        (uix/state 0)
        selected-elem-ref (uix/ref)]

    (key/bind! "j" ::cursor-down
               (fn [ev]
                 (swap! cursor-idx (fn [v]
                                     (let [new-v (inc v)]
                                       (if (> new-v (dec event-count))
                                         (dec event-count)
                                         new-v))))
                 (when selected-elem-ref
                   (println "hi!")
                   (.scrollIntoView @selected-elem-ref))))
    (key/bind! "k" ::cursor-up
               (fn [ev]
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
