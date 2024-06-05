(ns ralphie.trello
  (:require
   [cheshire.core :as json]
   [ralphie.config :as config]))

;; trello crud api

(def base-url "")
(def cards-url (str base-url "/cards"))

(defn trello-get [url _opts]
  (slurp url))

;; trello-json

(defonce trello-json (atom nil))

(defn get-trello-json []
  (println "getting trello json")
  (reset! trello-json
          (or @trello-json
              (do
                (println "ping-ing trello json url!!")
                (-> (config/trello-json-url)
                    slurp
                    (json/parse-string true)))))
  @trello-json)

(comment
  (get-trello-json)
  @trello-json
  )

;; get-cards


(defn get-cards
  "for some :list/name"
  ([] (get-cards nil))
  ([opts]
   (let [list-name (:list/name opts "Now")
         t-list    (some->> (get-trello-json) :lists
                            (filter (comp #{list-name} :name))
                            first
                            )]
     (if (not t-list)
       (println "No trello list found for :list/name" list-name)
       ;; break?
       (->>
         (get-trello-json) :cards
         (remove :closed)
         (filter (fn [card]
                   (= (:id t-list) (:idList card))))
         (map (fn [card]
                (assoc card
                       :card/id (:id card)
                       :card/date-last-activity (:dateLastActivity card)
                       :card/labels (:labels card)))))))

   ;; could return some cards likes
   #_[{:card/title    "some card name"
       :card/due-date nil
       :card/urls     []

       ;; pomodoro start/stop data
       :card/started-time nil ;; how long have we been working on this card

       :card/work-history
       [{:pomodoro/started-time  nil
         :pomodoro/finished-time nil
         :pomodoro/files-touched nil
         :pomodoro/urls-visited  nil
         :pomodoro/garden-notes  nil}]}]))

(comment
  (->>
    (get-cards {:list/name "Now"})
    )
  )


(println *ns* "<--- reloaded")
