(ns chess.core
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.curl :as curl]
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]

   [ralphie.zsh :as zsh]
   [wing.core :as w]))

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

  *lichess-env*
  )

(comment
  (reset! lichess-username "russmatney")
  (reset! lichess-token "") ;; paste and set token here
  )

(def lichess-api-base "https://lichess.org")
(def lichess-api-account (str lichess-api-base "/api/account"))
(def lichess-api-account-playing (str lichess-api-account "/playing"))
(def lichess-api-user (str lichess-api-base "/api/user"))
(def lichess-api-user-activity (str lichess-api-user "/" @lichess-username
                                    "/activity"))
;; (def lichess-api-puzzle-activity (str lichess-api-user
;;                                       "/puzzle-activity?max=5"))

(comment
  (curl/get
    lichess-api-account
    {:headers {:authorization (str "Bearer " @lichess-token)}
     :as      :json})

  (curl/get lichess-api-account-playing
            {:headers {:authorization (str "Bearer " @lichess-token)}
             :as      :json})


  (curl/get lichess-api-user-activity
            {:headers {:authorization (str "Bearer " @lichess-token)}
             :as      :json}))

(defn lichess-request
  "Makes a request to lichess.
  Starts the *lichess-env* to ensure username/token are available."
  [req-str]
  (println "Requesting from lichess!" req-str)
  (when-not (sys/running? `*lichess-env*)
    (sys/start! `*lichess-env*))
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
  (let [{:keys [id players opening clock createdAt lastMoveAt
                analysis]} gm]
    (-> gm
        (dissoc :players :opening :clock :createdAt :lastMoveAt)
        (#(w/ns-keys "lichess.game" %))

        (assoc
          :lichess.game/url (str "https://lichess.org/" id)

          :lichess.game/analysis (str analysis)

          :lichess.game/created-at-str createdAt
          :lichess.game/last-move-at-str lastMoveAt

          :lichess.game/opening-name (-> opening :name)
          :lichess.game/opening-eco (-> opening :eco)
          :lichess.game/opening-ply (-> opening :ply)

          :lichess.game/white-player (-> players :white :user :id)
          :lichess.game/black-player (-> players :black :user :id)
          :lichess.game/white-rating (-> players :white :rating)
          :lichess.game/black-rating (-> players :black :rating)
          :lichess.game/white-rating-diff (-> players :white :ratingDiff)
          :lichess.game/black-rating-diff (-> players :black :ratingDiff)

          :lichess.game/clock-initial (-> clock :initial)
          :lichess.game/clock-increment (-> clock :increment)
          :lichess.game/clock-total-time (-> clock :totalTime)

          :lichess/id id
          :lichess/url (str "https://lichess.org/" id)))))

(defn parse-lichess-json [raw]
  (-> raw (string/split #"\n")
      (->> (map #(json/parse-string % true)))))

(defonce *lichess-cache (atom {}))

(defn clear-cache []
  (reset! *lichess-cache {}))

(defn fetch-games
  ([]
   (fetch-games nil))
  ([{:keys [username max opening evals analysis since until literate]}]
   (println "Fetching lichess games")
   (when-not (sys/running? `*lichess-env*)
     (sys/start! `*lichess-env*))
   (let [max      (or max (when-not (or since until) 5))
         username (or username (:lichess/username *lichess-env*))
         endpoint+params
         (str lichess-api-base "/api/games/user/" username
              "?pgnInJson=true"
              (when max (str "&max=" max))
              (when opening "&opening=true")
              (when since (str "&since=" since))
              (when until (str "&until=" until))
              (when evals "&evals=true")
              (when literate "&literate=true")
              (when analysis "&analysis=true"))]
     (->
       (or (get @*lichess-cache endpoint+params)
           (lichess-request endpoint+params))
       ((fn [res] (swap! *lichess-cache #(assoc % endpoint+params res)) res))
       parse-lichess-json
       (->> (map parse-game))))))

(comment
  (fetch-games {:opening true
                :evals   true
                :max     5
                })

  (fetch-games)
  (clear-cache))

(defn parse-study [st]
  (let [{:keys [_pgn _id _players _opening]} st]
    (-> st
        ;; (assoc :lichess/id id
        ;;        :lichess/url (str "https://lichess.org/" id)
        ;;        :white-user (-> players :white :user :id)
        ;;        :black-user (-> players :black :user :id))
        )))

(defn fetch-studies
  ([]
   (fetch-studies nil))
  ([{:keys [username max] :study/keys [clocks comments variations]}]
   (println "Fetching lichess studies")
   (when-not (sys/running? `*lichess-env*)
     (sys/start! `*lichess-env*))
   (let [max      (or max 5)
         username (or username (:lichess/username *lichess-env*))
         endpoint+params
         (str lichess-api-base "/study/by/" username "/export.pgn"
              "?pgnInJson=true"
              "&max=" max
              (when clocks "&clocks=true")
              (when comments "&comments=true")
              (when variations "&variations=true"))]
     (->
       (or (get @*lichess-cache endpoint+params) (lichess-request endpoint+params))
       ((fn [res] (swap! *lichess-cache #(assoc % endpoint+params res)) res))
       parse-lichess-json
       (->> (map parse-study))))))

(comment
  (fetch-studies {:study/clocks     true
                  :study/comments   true
                  :study/variations true})

  ;; requires oauth
  (fetch-studies {:username "TeknqlTeam"})
  (fetch-studies)
  (clear-cache))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lichess Import
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def lichess-api-import (str lichess-api-base "/api/import"))

(comment

  (def bulk-pgns
    (-> (zsh/expand "~/Downloads/chess_com_games_2021-01-06.pgn")
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
       )
  )
