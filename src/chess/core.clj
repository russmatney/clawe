(ns chess.core
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.curl :as curl]
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]
   ))

(defonce lichess-username (atom nil))
(defonce lichess-token (atom nil))

(defn ->config [] (aero/read-config (io/resource "secret.edn")) )

(defsys *lichess-env*
  :start
  (let [{:lichess/keys [username token]} (->config)]
    {:lichess/username username
     :lichess/token    token}))

(comment
  (sys/start! '*lichess-env*)
  )

(comment
  (reset! lichess-username "russmatney")
  (reset! lichess-token "") ;; paste and set token here
  )

(def lichess-api-base "https://lichess.org/api")
(def lichess-api-account (str lichess-api-base "/account"))
(def lichess-api-account-playing (str lichess-api-account "/playing"))
(def lichess-api-user (str lichess-api-base "/user"))
(def lichess-api-user-activity (str lichess-api-user "/" @lichess-username
                                    "/activity"))
(def lichess-api-puzzle-activity (str lichess-api-user
                                      "/puzzle-activity?max=5"))

(comment
  (def --acct
    (curl/get
      lichess-api-account
      {:headers {:authorization (str "Bearer " @lichess-token)}
       :as      :json}))

  (def --account-current-games
    (curl/get lichess-api-account-playing
                {:headers {:authorization (str "Bearer " @lichess-token)}
                 :as      :json}))


  (def --user-activity-json
    (curl/get lichess-api-user-activity
                {:headers {:authorization (str "Bearer " @lichess-token)}
                 :as      :json})))

(defn lichess-request
  "Makes a request to lichess.
  Starts the *lichess-env* to ensure username/token are available.

  TODO check status and handle errors"
  [req-str]
  (println "Requesting from lichess!" req-str)
  ;; (sys/start! '*lichess-env*)
  (->> (curl/get
         req-str
         {:headers {:accept        "application/x-ndjson"
                    :authorization (str "Bearer "
                                        (:lichess/token *lichess-env*))}})
       :body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch lichess games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-game [gm]
  (let [{:keys [pgn id players opening]} gm]
    (-> gm
        (assoc :lichess/id id
               :lichess/url (str "https://lichess.org/" id)
               :white-user (-> players :white :user :id)
               :black-user (-> players :black :user :id)))))

(defonce *fetch-games-cache (atom {}))

(defn clear-cache []
  (reset! *fetch-games-cache nil))

(defn fetch-games
  ([]
   (fetch-games nil))
  ([{:keys [username max opening evals analysis]}]
   (println "Fetching lichess games")
   (when-not (sys/running? `*lichess-env*)
     (sys/start! `*lichess-env*))
   (let [max      (or max 5)
         username (or username (:lichess/username *lichess-env*))
         endpoint+params
         (str lichess-api-base "/games/user/" username
              "?pgnInJson=true"
              "&max=" max
              (when opening "&opening=true")
              (when evals "&evals=true")
              (when analysis "&analysis=true"))]

     (->
       (or (get @*fetch-games-cache endpoint+params) (lichess-request endpoint+params))
       ((fn [res]
          (swap! *fetch-games-cache #(assoc % endpoint+params res))
          res))
       (string/split #"\n")
       (->>
         (map (fn [raw-game]
                (-> raw-game
                    (json/parse-string true)
                    parse-game))))))))

(comment
  (def --user-games
    (fetch-games {:opening  true
                  :evals    true
                  :analysis true}))

  (fetch-games)
  (clear-cache)

  ;; TODO i ought to be storing these in clawe's db

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lichess Import
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def lichess-api-import (str lichess-api-base "/import"))

(comment

  (def bulk-pgns
    (-> "/home/russ/Downloads/chess_com_games_2021-01-06.pgn"
        slurp
        (string/split #"\n\n")
        (->>
          (partition 2 2)
          (map (partial string/join "\n\n")))))

  (count bulk-pgns)
  (->> bulk-pgns
       (map (comp first string/split-lines))
       (into #{})
       )

  (def some-pgn
    (-> bulk-pgns first))

  (curl/post
    lichess-api-import
    {:headers     {:authorization (str "Bearer " @lichess-token)
                   :accept        "application/x-www-form-urlencoded"}
     :form-params {:pgn some-pgn}})

  ;; rate limit is 200/hour with oauth!
  (doall
    (->> bulk-pgns
         (map
           (fn [pgn]
             (curl/post
               lichess-api-import
               {:headers     {:authorization (str "Bearer " @lichess-token)
                              :accept        "application/x-www-form-urlencoded"}
                :form-params {:pgn pgn}})))))

  (def res *1)

  (->> res
       (map :status)
       (into #{}))

  ;; format for import into study (one-url-per-line)
  ;; studies have a 64 chapter limit, but i'm just cutting to 50 for now
  (->> res
       ;; (take 50)
       (drop 50)
       (map :body)
       (map json/parse-string)
       (map #(get % "url"))
       (string/join "\n")
       (println)
       ;; and copy from repl...
       ;; TODO ought to just put it on my clipboard!
       )
  )
